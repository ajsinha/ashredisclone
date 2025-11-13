package com.ash.projects.redisclone.repository;

import com.ash.projects.redisclone.model.CacheEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RocksDB-based implementation of CacheRepositoryInterface.
 * Creates a separate RocksDB instance for each region.
 *
 * NOTE: This class is NOT annotated with @Repository.
 * It's managed as a bean by CacheRepositoryConfig based on configuration.
 *
 * @author ajsinha@gmail.com
 * Copyright (c) 2025 Ash Sinha. All rights reserved.
 */
public class CacheRepositoryRocksDB implements CacheRepositoryInterface {

    private static final Logger logger = LoggerFactory.getLogger(CacheRepositoryRocksDB.class);
    private static final String KEY_SEPARATOR = "::";

    @Value("${cache.rocksdb.base.path:./data/rocksdb}")
    private String baseRocksDbPath;

    @Value("${cache.rocksdb.parallel.loading.enabled:true}")
    private boolean parallelLoadingEnabled;

    @Value("${cache.rocksdb.parallel.loading.threads:4}")
    private int parallelLoadingThreads;

    @Value("${cache.rocksdb.parallel.loading.timeout.seconds:300}")
    private int parallelLoadingTimeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, RocksDB> regionDatabases = new ConcurrentHashMap<>();
    private final Map<String, Options> regionOptions = new ConcurrentHashMap<>();

    static {
        // Load RocksDB native library
        RocksDB.loadLibrary();
    }

    /**
     * Constructor with configurable base path
     */
    public CacheRepositoryRocksDB(String baseRocksDbPath) {
        this.baseRocksDbPath = baseRocksDbPath;
    }

    /**
     * Default constructor for Spring
     */
    public CacheRepositoryRocksDB() {
    }

    @PostConstruct
    @Override
    public void initializeDatabase() {
        logger.info("Initializing RocksDB cache repository at: {}", baseRocksDbPath);

        try {
            // Create base directory if it doesn't exist
            Path basePath = Paths.get(baseRocksDbPath);
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                logger.info("Created RocksDB base directory: {}", baseRocksDbPath);
            }

            // Load existing regions
            loadExistingRegions();

            logger.info("RocksDB cache repository initialized successfully");
        } catch (IOException e) {
            logger.error("Failed to initialize RocksDB base directory", e);
            throw new RuntimeException("Failed to initialize RocksDB", e);
        }
    }

    /**
     * Load existing region databases from disk
     * Uses parallel loading if enabled for faster startup
     */
    private void loadExistingRegions() {
        try {
            File baseDir = new File(baseRocksDbPath);
            if (!baseDir.exists()) {
                return;
            }

            File[] regionDirs = baseDir.listFiles(File::isDirectory);
            if (regionDirs == null || regionDirs.length == 0) {
                logger.info("No existing regions found");
                return;
            }

            if (parallelLoadingEnabled && regionDirs.length > 1) {
                loadRegionsInParallel(regionDirs);
            } else {
                loadRegionsSequentially(regionDirs);
            }

        } catch (Exception e) {
            logger.error("Error loading existing regions", e);
        }
    }

    /**
     * Load regions sequentially (original method)
     */
    private void loadRegionsSequentially(File[] regionDirs) {
        logger.info("Loading {} regions sequentially...", regionDirs.length);
        long startTime = System.currentTimeMillis();

        int successCount = 0;
        int failureCount = 0;

        for (File regionDir : regionDirs) {
            String regionName = regionDir.getName();
            try {
                getOrCreateRegionDb(regionName);
                logger.info("Loaded existing region: {}", regionName);
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to load region: {}", regionName, e);
                failureCount++;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Sequential loading completed: {} successful, {} failed in {}ms",
                successCount, failureCount, duration);
    }

    /**
     * Load regions in parallel using thread pool for faster startup
     */
    private void loadRegionsInParallel(File[] regionDirs) {
        int threadCount = Math.min(parallelLoadingThreads, regionDirs.length);
        logger.info("Loading {} regions in parallel using {} threads...",
                regionDirs.length, threadCount);

        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(
                threadCount,
                r -> {
                    Thread t = new Thread(r);
                    t.setName("RocksDB-Region-Loader-" + t.getId());
                    t.setDaemon(true);
                    return t;
                }
        );

        CountDownLatch latch = new CountDownLatch(regionDirs.length);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        try {
            // Submit all region loading tasks
            for (File regionDir : regionDirs) {
                String regionName = regionDir.getName();
                executor.submit(() -> {
                    try {
                        long regionStartTime = System.currentTimeMillis();
                        getOrCreateRegionDb(regionName);
                        long regionDuration = System.currentTimeMillis() - regionStartTime;

                        logger.info("Loaded region '{}' in {}ms", regionName, regionDuration);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        logger.error("Failed to load region: {}", regionName, e);
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all regions to load or timeout
            boolean completed = latch.await(parallelLoadingTimeoutSeconds, TimeUnit.SECONDS);

            if (!completed) {
                logger.warn("Region loading timeout after {} seconds. {} regions may not be loaded.",
                        parallelLoadingTimeoutSeconds, latch.getCount());
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            logger.info("Parallel loading completed: {} successful, {} failed in {}ms (avg {}ms per region)",
                    successCount.get(), failureCount.get(), totalDuration,
                    totalDuration / regionDirs.length);

        } catch (InterruptedException e) {
            logger.error("Region loading interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Get or create RocksDB instance for a region
     */
    private synchronized RocksDB getOrCreateRegionDb(String region) throws RocksDBException {
        return regionDatabases.computeIfAbsent(region, r -> {
            try {
                String regionPath = getRegionPath(r);

                // Create directory for region if it doesn't exist
                File regionDir = new File(regionPath);
                if (!regionDir.exists()) {
                    regionDir.mkdirs();
                }

                // Configure RocksDB options
                Options options = new Options()
                        .setCreateIfMissing(true)
                        .setCompressionType(CompressionType.LZ4_COMPRESSION)
                        .setWriteBufferSize(64 * 1024 * 1024) // 64MB
                        .setMaxWriteBufferNumber(3)
                        .setMaxBackgroundCompactions(4)
                        .setMaxBackgroundFlushes(2);

                regionOptions.put(r, options);

                RocksDB db = RocksDB.open(options, regionPath);
                logger.info("Created/Opened RocksDB for region: {} at {}", r, regionPath);

                return db;
            } catch (RocksDBException e) {
                logger.error("Failed to create RocksDB for region: {}", r, e);
                throw new RuntimeException("Failed to create RocksDB for region: " + r, e);
            }
        });
    }

    /**
     * Get the file system path for a region's RocksDB
     */
    private String getRegionPath(String region) {
        return baseRocksDbPath + File.separator + region;
    }

    /**
     * Create a composite key from region and key
     */
    private byte[] createCompositeKey(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void saveEntry(CacheEntry entry) {
        try {
            RocksDB db = getOrCreateRegionDb(entry.getRegion());

            // Serialize the entire CacheEntry to JSON
            String entryJson = objectMapper.writeValueAsString(entry);
            byte[] keyBytes = createCompositeKey(entry.getKey());
            byte[] valueBytes = entryJson.getBytes(StandardCharsets.UTF_8);

            // Write to RocksDB
            db.put(keyBytes, valueBytes);

            logger.debug("Saved entry: region={}, key={}", entry.getRegion(), entry.getKey());
        } catch (Exception e) {
            logger.error("Error saving entry: region={}, key={}", entry.getRegion(), entry.getKey(), e);
        }
    }

    @Override
    public CacheEntry loadEntry(String region, String key) {
        try {
            RocksDB db = getOrCreateRegionDb(region);
            byte[] keyBytes = createCompositeKey(key);
            byte[] valueBytes = db.get(keyBytes);

            if (valueBytes == null) {
                return null;
            }

            String entryJson = new String(valueBytes, StandardCharsets.UTF_8);
            CacheEntry entry = objectMapper.readValue(entryJson, CacheEntry.class);

            logger.debug("Loaded entry: region={}, key={}", region, key);
            return entry;

        } catch (Exception e) {
            logger.error("Error loading entry: region={}, key={}", region, key, e);
            return null;
        }
    }

    @Override
    public List<CacheEntry> loadAllEntries() {
        List<CacheEntry> allEntries = new ArrayList<>();

        for (String region : regionDatabases.keySet()) {
            allEntries.addAll(loadEntriesByRegion(region));
        }

        logger.debug("Loaded {} total entries from all regions", allEntries.size());
        return allEntries;
    }

    @Override
    public List<CacheEntry> loadEntriesByRegion(String region) {
        List<CacheEntry> entries = new ArrayList<>();

        try {
            RocksDB db = getOrCreateRegionDb(region);

            // Use iterator to scan all entries in the region
            try (RocksIterator iterator = db.newIterator()) {
                iterator.seekToFirst();

                while (iterator.isValid()) {
                    byte[] valueBytes = iterator.value();
                    String entryJson = new String(valueBytes, StandardCharsets.UTF_8);

                    try {
                        CacheEntry entry = objectMapper.readValue(entryJson, CacheEntry.class);
                        entries.add(entry);
                    } catch (Exception e) {
                        logger.error("Error deserializing entry in region: {}", region, e);
                    }

                    iterator.next();
                }
            }

            logger.debug("Loaded {} entries from region: {}", entries.size(), region);
        } catch (Exception e) {
            logger.error("Error loading entries for region: {}", region, e);
        }

        return entries;
    }

    @Override
    public void deleteEntry(String region, String key) {
        try {
            RocksDB db = getOrCreateRegionDb(region);
            byte[] keyBytes = createCompositeKey(key);
            db.delete(keyBytes);

            logger.debug("Deleted entry: region={}, key={}", region, key);
        } catch (Exception e) {
            logger.error("Error deleting entry: region={}, key={}", region, key, e);
        }
    }

    @Override
    public void deleteRegion(String region) {
        try {
            RocksDB db = regionDatabases.get(region);

            if (db != null) {
                // Close the database first
                db.close();
                regionDatabases.remove(region);

                // Close and remove options
                Options options = regionOptions.remove(region);
                if (options != null) {
                    options.close();
                }

                // Delete the directory
                String regionPath = getRegionPath(region);
                deleteDirectory(new File(regionPath));

                logger.info("Deleted region: {}", region);
            }
        } catch (Exception e) {
            logger.error("Error deleting region: {}", region, e);
        }
    }

    /**
     * Recursively delete a directory
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        Files.delete(file.toPath());
                    }
                }
            }
            Files.delete(directory.toPath());
        }
    }

    @Override
    public void updateExpiry(String region, String key, Long expiresAt) {
        try {
            // Load the entry, update expiry, and save it back
            CacheEntry entry = loadEntry(region, key);
            if (entry != null) {
                entry.setExpiresAt(expiresAt);
                saveEntry(entry);
                logger.debug("Updated expiry: region={}, key={}, expiresAt={}", region, key, expiresAt);
            }
        } catch (Exception e) {
            logger.error("Error updating expiry: region={}, key={}", region, key, e);
        }
    }

    @Override
    public void deleteExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        int totalDeleted = 0;

        for (String region : regionDatabases.keySet()) {
            int deletedInRegion = deleteExpiredEntriesInRegion(region, currentTime);
            totalDeleted += deletedInRegion;
        }

        if (totalDeleted > 0) {
            logger.info("Deleted {} expired entries across all regions", totalDeleted);
        }
    }

    /**
     * Delete expired entries in a specific region
     */
    private int deleteExpiredEntriesInRegion(String region, long currentTime) {
        int deleted = 0;
        List<String> keysToDelete = new ArrayList<>();

        try {
            RocksDB db = getOrCreateRegionDb(region);

            try (RocksIterator iterator = db.newIterator()) {
                iterator.seekToFirst();

                while (iterator.isValid()) {
                    byte[] valueBytes = iterator.value();
                    String entryJson = new String(valueBytes, StandardCharsets.UTF_8);

                    try {
                        CacheEntry entry = objectMapper.readValue(entryJson, CacheEntry.class);

                        if (entry.getExpiresAt() != null && entry.getExpiresAt() < currentTime) {
                            keysToDelete.add(entry.getKey());
                        }
                    } catch (Exception e) {
                        logger.error("Error checking expiry for entry in region: {}", region, e);
                    }

                    iterator.next();
                }
            }

            // Delete expired entries
            for (String key : keysToDelete) {
                deleteEntry(region, key);
                deleted++;
            }

        } catch (Exception e) {
            logger.error("Error deleting expired entries in region: {}", region, e);
        }

        return deleted;
    }

    @Override
    public long getEntryCount(String region) {
        try {
            RocksDB db = getOrCreateRegionDb(region);
            long count = 0;

            try (RocksIterator iterator = db.newIterator()) {
                iterator.seekToFirst();
                while (iterator.isValid()) {
                    count++;
                    iterator.next();
                }
            }

            return count;
        } catch (Exception e) {
            logger.error("Error getting entry count for region: {}", region, e);
            return 0;
        }
    }

    /**
     * Get all region names
     */
    public Set<String> getAllRegions() {
        return new HashSet<>(regionDatabases.keySet());
    }

    /**
     * Get statistics for a region
     */
    public String getRegionStats(String region) {
        try {
            RocksDB db = regionDatabases.get(region);
            if (db != null) {
                return db.getProperty("rocksdb.stats");
            }
        } catch (RocksDBException e) {
            logger.error("Error getting stats for region: {}", region, e);
        }
        return "No stats available";
    }

    /**
     * Compact a region's database
     */
    public void compactRegion(String region) {
        try {
            RocksDB db = regionDatabases.get(region);
            if (db != null) {
                db.compactRange();
                logger.info("Compacted region: {}", region);
            }
        } catch (RocksDBException e) {
            logger.error("Error compacting region: {}", region, e);
        }
    }

    /**
     * Flush all pending writes to disk
     */
    public void flush() {
        for (Map.Entry<String, RocksDB> entry : regionDatabases.entrySet()) {
            try {
                FlushOptions flushOptions = new FlushOptions().setWaitForFlush(true);
                entry.getValue().flush(flushOptions);
                flushOptions.close();
                logger.debug("Flushed region: {}", entry.getKey());
            } catch (RocksDBException e) {
                logger.error("Error flushing region: {}", entry.getKey(), e);
            }
        }
    }

    /**
     * Close all RocksDB instances and clean up resources
     */
    @PreDestroy
    public void close() {
        logger.info("Closing RocksDB cache repository");

        for (Map.Entry<String, RocksDB> entry : regionDatabases.entrySet()) {
            try {
                entry.getValue().close();
                logger.debug("Closed RocksDB for region: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Error closing RocksDB for region: {}", entry.getKey(), e);
            }
        }

        // Close all options
        for (Map.Entry<String, Options> entry : regionOptions.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                logger.error("Error closing options for region: {}", entry.getKey(), e);
            }
        }

        regionDatabases.clear();
        regionOptions.clear();

        logger.info("RocksDB cache repository closed");
    }
}