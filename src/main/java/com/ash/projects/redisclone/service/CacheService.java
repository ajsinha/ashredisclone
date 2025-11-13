package com.ash.projects.redisclone.service;

import com.ash.projects.redisclone.model.*;
import com.ash.projects.redisclone.repository.CacheRepositoryInterface;
// CHANGED: Import interface instead of concrete implementation
// OLD: import com.ash.projects.redisclone.repository.CacheRepositorySQL;
// NEW: import com.ash.projects.redisclone.repository.CacheRepositoryInterface;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Cache Service - Updated to use config-driven repository
 *
 * CHANGES FROM ORIGINAL:
 * - Line 4: Changed from CacheRepositorySQL to CacheRepositoryInterface
 * - Line 34: Inject CacheRepositoryInterface instead of CacheRepositorySQL
 *
 * The actual implementation (SQL or RocksDB) is determined by application configuration.
 * No other code changes are required!
 *
 * @author ajsinha@gmail.com
 * Copyright (c) 2025 Ash Sinha. All rights reserved.
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    @Value("${cache.max.memory.objects:100000}")
    private int maxMemoryObjects;

    @Value("${cache.default.region:region0}")
    private String defaultRegion;

    // CHANGED: Inject interface instead of concrete implementation
    @Autowired
    private CacheRepositoryInterface cacheRepository;
    // OLD: private CacheRepositorySQL cacheRepository;

    @Lazy
    @Autowired(required = false)
    private ReplicationService replicationService;

    @Lazy
    @Autowired(required = false)
    private PubSubService pubSubService;

    // Region -> (Key -> CacheEntry)
    private final Map<String, Map<String, CacheEntry>> memoryCache = new ConcurrentHashMap<>();

    // Region -> Set of all keys (including those in DB)
    private final Map<String, Set<String>> allKeys = new ConcurrentHashMap<>();

    // LRU tracking
    private final Map<String, LinkedHashMap<String, Long>> lruTracking = new ConcurrentHashMap<>();

    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    private final Map<String, ReadWriteLock> regionLocks = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        logger.info("Initializing Cache Service with max memory objects: {}", maxMemoryObjects);

        // Ensure default region exists
        getOrCreateRegion(defaultRegion);

        // Prime cache from database
        primeCacheFromDatabase();

        logger.info("Cache Service initialized successfully");
    }

    private void primeCacheFromDatabase() {
        try {
            logger.info("Priming cache from database...");
            List<CacheEntry> allEntries = cacheRepository.loadAllEntries();

            // Track unique regions found in database
            Set<String> regionsFound = new HashSet<>();

            for (CacheEntry entry : allEntries) {
                String region = entry.getRegion() != null ? entry.getRegion() : defaultRegion;

                // Track this region
                regionsFound.add(region);

                // Add key to allKeys
                allKeys.computeIfAbsent(region, k -> ConcurrentHashMap.newKeySet()).add(entry.getKey());

                // Load into memory if space available
                if (getCurrentMemoryObjectCount() < maxMemoryObjects) {
                    loadEntryIntoMemory(entry);
                }
            }

            // CRITICAL: Initialize all regions found in database to ensure proper data structure setup
            // This ensures memoryCache, lruTracking, and allKeys are properly initialized
            // even if no entries are loaded into memory for a region
            for (String region : regionsFound) {
                getOrCreateRegion(region);
            }

            logger.info("Cache primed with {} entries from {} regions in database",
                    allEntries.size(), regionsFound.size());
        } catch (Exception e) {
            logger.error("Error priming cache from database", e);
        }
    }

    private void loadEntryIntoMemory(CacheEntry entry) {
        String region = entry.getRegion();
        memoryCache.computeIfAbsent(region, k -> new ConcurrentHashMap<>()).put(entry.getKey(), entry);
        entry.setInMemory(true);
        updateLRU(region, entry.getKey());
    }

    private ReadWriteLock getRegionLock(String region) {
        return regionLocks.computeIfAbsent(region, k -> new ReentrantReadWriteLock());
    }

    public String getDefaultRegion() {
        return defaultRegion;
    }

    private void getOrCreateRegion(String region) {
        memoryCache.computeIfAbsent(region, k -> new ConcurrentHashMap<>());
        allKeys.computeIfAbsent(region, k -> ConcurrentHashMap.newKeySet());
        lruTracking.computeIfAbsent(region, k -> new LinkedHashMap<>(16, 0.75f, true));
    }

    // SET operation
    public boolean set(String region, String key, String value, Long expiresAt) {
        region = region != null ? region : defaultRegion;
        getOrCreateRegion(region);

        ReadWriteLock lock = getRegionLock(region);
        lock.writeLock().lock();
        try {
            CacheEntry entry = new CacheEntry(key, region, DataType.STRING, value);
            entry.setExpiresAt(expiresAt);

            putEntry(region, key, entry);

            // Replicate if enabled - wrap entire block to handle lazy proxy
            try {
                if (replicationService != null) {
                    replicationService.replicateSet(region, key, value, DataType.STRING, expiresAt);
                }
            } catch (Exception e) {
                logger.warn("Failed to replicate SET operation: {}", e.getMessage());
            }

            // Publish event if enabled - wrap entire block to handle lazy proxy
            try {
                if (pubSubService != null) {
                    pubSubService.publishChange(region, key, "SET");
                }
            } catch (Exception e) {
                logger.warn("Failed to publish change event: {}", e.getMessage());
            }

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // GET operation
    public String get(String region, String key) {
        region = region != null ? region : defaultRegion;

        ReadWriteLock lock = getRegionLock(region);
        lock.readLock().lock();
        try {
            CacheEntry entry = getEntry(region, key);
            if (entry == null || entry.isExpired()) {
                return null;
            }

            if (entry.getDataType() != DataType.STRING) {
                return null;
            }

            entry.updateAccessTime();
            updateLRU(region, key);

            return (String) entry.getValue();
        } finally {
            lock.readLock().unlock();
        }
    }

    // DELETE operation
    public long del(String region, String... keys) {
        region = region != null ? region : defaultRegion;

        ReadWriteLock lock = getRegionLock(region);
        lock.writeLock().lock();
        try {
            long deleted = 0;
            for (String key : keys) {
                if (deleteEntry(region, key)) {
                    deleted++;

                    // Replicate if enabled - wrap entire block to handle lazy proxy
                    try {
                        if (replicationService != null) {
                            replicationService.replicateDelete(region, key);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to replicate DELETE operation: {}", e.getMessage());
                    }

                    // Publish event if enabled - wrap entire block to handle lazy proxy
                    try {
                        if (pubSubService != null) {
                            pubSubService.publishChange(region, key, "DEL");
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to publish change event: {}", e.getMessage());
                    }
                }
            }
            return deleted;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // EXISTS operation
    public long exists(String region, String... keys) {
        region = region != null ? region : defaultRegion;

        ReadWriteLock lock = getRegionLock(region);
        lock.readLock().lock();
        try {
            long count = 0;
            for (String key : keys) {
                Set<String> regionKeys = allKeys.get(region);
                if (regionKeys != null && regionKeys.contains(key)) {
                    CacheEntry entry = getEntry(region, key);
                    if (entry != null && !entry.isExpired()) {
                        count++;
                    }
                }
            }
            return count;
        } finally {
            lock.readLock().unlock();
        }
    }

    // EXPIRE operation
    public boolean expire(String region, String key, long seconds) {
        region = region != null ? region : defaultRegion;

        ReadWriteLock lock = getRegionLock(region);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getEntry(region, key);
            if (entry == null) {
                return false;
            }

            long expiresAt = System.currentTimeMillis() + (seconds * 1000);
            entry.setExpiresAt(expiresAt);

            // Update in database
            cacheRepository.updateExpiry(region, key, expiresAt);

            // Replicate if enabled - wrap entire block to handle lazy proxy
            try {
                if (replicationService != null) {
                    replicationService.replicateExpire(region, key, seconds);
                }
            } catch (Exception e) {
                logger.warn("Failed to replicate EXPIRE operation: {}", e.getMessage());
            }

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // TTL operation
    public long ttl(String region, String key) {
        region = region != null ? region : defaultRegion;

        ReadWriteLock lock = getRegionLock(region);
        lock.readLock().lock();
        try {
            CacheEntry entry = getEntry(region, key);
            if (entry == null) {
                return -2; // Key does not exist
            }

            return entry.getTtlSeconds();
        } finally {
            lock.readLock().unlock();
        }
    }

    // PERSIST operation
    public boolean persist(String region, String key) {
        region = region != null ? region : defaultRegion;

        ReadWriteLock lock = getRegionLock(region);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getEntry(region, key);
            if (entry == null) {
                return false;
            }

            entry.setExpiresAt(null);
            cacheRepository.updateExpiry(region, key, null);

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // KEYS pattern operation
    public Set<String> keys(String region, String pattern) {
        region = region != null ? region : defaultRegion;

        ReadWriteLock lock = getRegionLock(region);
        lock.readLock().lock();
        try {
            Set<String> regionKeys = allKeys.get(region);
            if (regionKeys == null) {
                return Collections.emptySet();
            }

            Pattern regexPattern = convertGlobToRegex(pattern);
            return regionKeys.stream()
                    .filter(key -> regexPattern.matcher(key).matches())
                    .collect(Collectors.toSet());
        } finally {
            lock.readLock().unlock();
        }
    }

    private Pattern convertGlobToRegex(String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        return Pattern.compile(regex);
    }

    // Helper methods
    private void putEntry(String region, String key, CacheEntry entry) {
        allKeys.computeIfAbsent(region, k -> ConcurrentHashMap.newKeySet()).add(key);

        if (getCurrentMemoryObjectCount() >= maxMemoryObjects) {
            evictLRUEntry();
        }

        memoryCache.get(region).put(key, entry);
        entry.setInMemory(true);
        updateLRU(region, key);

        // Persist to database
        cacheRepository.saveEntry(entry);
    }

    private CacheEntry getEntry(String region, String key) {
        Map<String, CacheEntry> regionCache = memoryCache.get(region);
        if (regionCache != null) {
            CacheEntry entry = regionCache.get(key);
            if (entry != null) {
                return entry;
            }
        }

        // Try loading from database
        CacheEntry entry = cacheRepository.loadEntry(region, key);
        if (entry != null && !entry.isExpired()) {
            if (getCurrentMemoryObjectCount() < maxMemoryObjects) {
                loadEntryIntoMemory(entry);
            }
            return entry;
        }

        return null;
    }

    private boolean deleteEntry(String region, String key) {
        Set<String> regionKeys = allKeys.get(region);
        if (regionKeys != null) {
            regionKeys.remove(key);
        }

        Map<String, CacheEntry> regionCache = memoryCache.get(region);
        if (regionCache != null) {
            regionCache.remove(key);
        }

        removeLRU(region, key);
        cacheRepository.deleteEntry(region, key);

        return true;
    }

    private void updateLRU(String region, String key) {
        // Get or create LRU tracking for region
        LinkedHashMap<String, Long> regionLRU = lruTracking.computeIfAbsent(
                region,
                k -> new LinkedHashMap<>(16, 0.75f, true)
        );

        synchronized (regionLRU) {
            regionLRU.put(key, System.currentTimeMillis());
        }
    }

    private void removeLRU(String region, String key) {
        LinkedHashMap<String, Long> regionLRU = lruTracking.get(region);
        if (regionLRU != null) {
            synchronized (regionLRU) {
                regionLRU.remove(key);
            }
        }
    }

    private void evictLRUEntry() {
        String oldestRegion = null;
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;

        for (Map.Entry<String, LinkedHashMap<String, Long>> regionEntry : lruTracking.entrySet()) {
            LinkedHashMap<String, Long> regionLRU = regionEntry.getValue();
            synchronized (regionLRU) {
                if (!regionLRU.isEmpty()) {
                    Map.Entry<String, Long> first = regionLRU.entrySet().iterator().next();
                    if (first.getValue() < oldestTime) {
                        oldestTime = first.getValue();
                        oldestRegion = regionEntry.getKey();
                        oldestKey = first.getKey();
                    }
                }
            }
        }

        if (oldestRegion != null && oldestKey != null) {
            evictToDatabase(oldestRegion, oldestKey);
        }
    }

    private void evictToDatabase(String region, String key) {
        Map<String, CacheEntry> regionCache = memoryCache.get(region);
        if (regionCache != null) {
            CacheEntry entry = regionCache.remove(key);
            if (entry != null) {
                entry.setInMemory(false);
                cacheRepository.saveEntry(entry);
                removeLRU(region, key);
                logger.debug("Evicted entry: region={}, key={}", region, key);
            }
        }
    }

    private int getCurrentMemoryObjectCount() {
        return memoryCache.values().stream()
                .mapToInt(Map::size)
                .sum();
    }

    // Scheduled cleanup of expired entries
    @Scheduled(fixedDelayString = "${cache.cleanup.interval.seconds:60}000")
    public void cleanupExpiredEntries() {
        logger.debug("Running expired entries cleanup");

        int cleanedCount = 0;

        for (String region : new HashSet<>(allKeys.keySet())) {
            Set<String> regionKeys = new HashSet<>(allKeys.getOrDefault(region, Collections.emptySet()));

            for (String key : regionKeys) {
                CacheEntry entry = getEntry(region, key);
                if (entry != null && entry.isExpired()) {
                    deleteEntry(region, key);
                    cleanedCount++;
                }
            }
        }

        if (cleanedCount > 0) {
            logger.info("Cleaned up {} expired entries", cleanedCount);
        }
    }

    public Set<String> getAllRegions() {
        // Return all regions that have any keys (in memory or in database)
        // This ensures we see all regions even if no entries are currently in memory
        Set<String> regions = new HashSet<>();
        regions.addAll(memoryCache.keySet());
        regions.addAll(allKeys.keySet());
        return regions;
    }

    /**
     * Delete an entire region and all its keys
     * This method cleans up:
     * 1. All entries from the database
     * 2. All in-memory cache entries
     * 3. All metadata (allKeys, lruTracking, regionLocks, memoryCache)
     *
     * @param region The region to delete
     */
    public void deleteRegion(String region) {
        if (region == null || region.equals(defaultRegion)) {
            throw new IllegalArgumentException("Cannot delete default region");
        }

        logger.info("Deleting region: {}", region);

        ReadWriteLock lock = getRegionLock(region);
        lock.writeLock().lock();
        try {
            // 1. Replicate if enabled
            if (replicationService != null) {
                try {
                    replicationService.replicateDeleteRegion(region);
                } catch (Exception e) {
                    logger.warn("Failed to replicate DELETE_REGION operation: {}", e.getMessage());
                }
            }

            // 2. Delete from database
            cacheRepository.deleteRegion(region);
            logger.debug("Deleted region '{}' from database", region);

            // 3. Clean up in-memory structures
            memoryCache.remove(region);
            allKeys.remove(region);
            lruTracking.remove(region);
            regionLocks.remove(region);

            // 4. Publish event if enabled
            if (pubSubService != null) {
                try {
                    pubSubService.publishChange(region, "*", "DELETE_REGION");
                } catch (Exception e) {
                    logger.warn("Failed to publish DELETE_REGION event: {}", e.getMessage());
                }
            }

            logger.info("Region '{}' deleted successfully", region);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<String, Object> getRegionStats(String region) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("name", region);
        stats.put("totalKeys", allKeys.getOrDefault(region, Collections.emptySet()).size());
        stats.put("memoryKeys", memoryCache.getOrDefault(region, Collections.emptyMap()).size());
        return stats;
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Cache Service");
    }
}