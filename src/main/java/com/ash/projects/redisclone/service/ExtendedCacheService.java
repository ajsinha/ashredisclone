package com.ash.projects.redisclone.service;

import com.ash.projects.redisclone.model.*;
import com.ash.projects.redisclone.repository.CacheRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Extended cache operations for Hash, List, Set, and Sorted Set data types
 */
@Service
public class ExtendedCacheService {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CacheRepository cacheRepository;

    private final Map<String, ReadWriteLock> typeLocks = new ConcurrentHashMap<>();

    private ReadWriteLock getLock(String region, String key) {
        String lockKey = region + ":" + key;
        return typeLocks.computeIfAbsent(lockKey, k -> new ReentrantReadWriteLock());
    }

    // ==================== HASH OPERATIONS ====================

    /**
     * HSET - Set hash field value
     */
    public long hset(String region, String key, String field, String value) {
        Map<String, String> fieldValues = new HashMap<>();
        fieldValues.put(field, value);
        return hset(region, key, fieldValues);
    }

    public long hset(String region, String key, Map<String, String> fieldValues) {
        ReadWriteLock lock = getLock(region, key);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getOrCreateEntry(region, key, DataType.HASH);

            @SuppressWarnings("unchecked")
            Map<String, String> hash = (Map<String, String>) entry.getValue();
            if (hash == null) {
                hash = new ConcurrentHashMap<>();
                entry.setValue(hash);
            }

            int added = 0;
            for (Map.Entry<String, String> fieldValue : fieldValues.entrySet()) {
                if (!hash.containsKey(fieldValue.getKey())) {
                    added++;
                }
                hash.put(fieldValue.getKey(), fieldValue.getValue());
            }

            saveEntry(entry);
            return added;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * HGET - Get hash field value
     */
    public String hget(String region, String key, String field) {
        ReadWriteLock lock = getLock(region, key);
        lock.readLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.HASH);
            if (entry == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, String> hash = (Map<String, String>) entry.getValue();
            return hash != null ? hash.get(field) : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * HGETALL - Get all fields and values
     */
    public Map<String, String> hgetAll(String region, String key) {
        ReadWriteLock lock = getLock(region, key);
        lock.readLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.HASH);
            if (entry == null) {
                return new HashMap<>();
            }

            @SuppressWarnings("unchecked")
            Map<String, String> hash = (Map<String, String>) entry.getValue();
            return hash != null ? new HashMap<>(hash) : new HashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * HDEL - Delete hash fields
     */
    public long hdel(String region, String key, String... fields) {
        ReadWriteLock lock = getLock(region, key);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.HASH);
            if (entry == null) {
                return 0;
            }

            @SuppressWarnings("unchecked")
            Map<String, String> hash = (Map<String, String>) entry.getValue();
            if (hash == null) {
                return 0;
            }

            long deleted = 0;
            for (String field : fields) {
                if (hash.remove(field) != null) {
                    deleted++;
                }
            }

            saveEntry(entry);
            return deleted;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== LIST OPERATIONS ====================

    /**
     * LPUSH - Push to head of list
     */
    public long lpush(String region, String key, String... values) {
        ReadWriteLock lock = getLock(region, key);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getOrCreateEntry(region, key, DataType.LIST);

            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) entry.getValue();
            if (list == null) {
                list = new ArrayList<>();
                entry.setValue(list);
            }

            for (int i = values.length - 1; i >= 0; i--) {
                list.add(0, values[i]);
            }

            saveEntry(entry);
            return list.size();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * RPUSH - Push to tail of list
     */
    public long rpush(String region, String key, String... values) {
        ReadWriteLock lock = getLock(region, key);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getOrCreateEntry(region, key, DataType.LIST);

            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) entry.getValue();
            if (list == null) {
                list = new ArrayList<>();
                entry.setValue(list);
            }

            list.addAll(Arrays.asList(values));

            saveEntry(entry);
            return list.size();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * LPOP - Pop from head of list
     */
    public String lpop(String region, String key) {
        ReadWriteLock lock = getLock(region, key);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.LIST);
            if (entry == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) entry.getValue();
            if (list == null || list.isEmpty()) {
                return null;
            }

            String value = list.remove(0);
            saveEntry(entry);
            return value;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * RPOP - Pop from tail of list
     */
    public String rpop(String region, String key) {
        ReadWriteLock lock = getLock(region, key);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.LIST);
            if (entry == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) entry.getValue();
            if (list == null || list.isEmpty()) {
                return null;
            }

            String value = list.remove(list.size() - 1);
            saveEntry(entry);
            return value;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * LRANGE - Get range of elements
     */
    public List<String> lrange(String region, String key, int start, int stop) {
        ReadWriteLock lock = getLock(region, key);
        lock.readLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.LIST);
            if (entry == null) {
                return new ArrayList<>();
            }

            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) entry.getValue();
            if (list == null || list.isEmpty()) {
                return new ArrayList<>();
            }

            int size = list.size();
            int fromIndex = start < 0 ? Math.max(0, size + start) : Math.min(start, size);
            int toIndex = stop < 0 ? Math.max(0, size + stop + 1) : Math.min(stop + 1, size);

            if (fromIndex >= toIndex) {
                return new ArrayList<>();
            }

            return new ArrayList<>(list.subList(fromIndex, toIndex));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * LLEN - Get list length
     */
    public long llen(String region, String key) {
        ReadWriteLock lock = getLock(region, key);
        lock.readLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.LIST);
            if (entry == null) {
                return 0;
            }

            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) entry.getValue();
            return list != null ? list.size() : 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== SET OPERATIONS ====================

    /**
     * SADD - Add members to set
     */
    public long sadd(String region, String key, String... members) {
        ReadWriteLock lock = getLock(region, key);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getOrCreateEntry(region, key, DataType.SET);

            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) entry.getValue();
            if (set == null) {
                set = new HashSet<>();
                entry.setValue(set);
            }

            long added = 0;
            for (String member : members) {
                if (set.add(member)) {
                    added++;
                }
            }

            saveEntry(entry);
            return added;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * SMEMBERS - Get all set members
     */
    public Set<String> smembers(String region, String key) {
        ReadWriteLock lock = getLock(region, key);
        lock.readLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.SET);
            if (entry == null) {
                return new HashSet<>();
            }

            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) entry.getValue();
            return set != null ? new HashSet<>(set) : new HashSet<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * SREM - Remove members from set
     */
    public long srem(String region, String key, String... members) {
        ReadWriteLock lock = getLock(region, key);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.SET);
            if (entry == null) {
                return 0;
            }

            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) entry.getValue();
            if (set == null) {
                return 0;
            }

            long removed = 0;
            for (String member : members) {
                if (set.remove(member)) {
                    removed++;
                }
            }

            saveEntry(entry);
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * SISMEMBER - Check if member exists in set
     */
    public boolean sismember(String region, String key, String member) {
        ReadWriteLock lock = getLock(region, key);
        lock.readLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.SET);
            if (entry == null) {
                return false;
            }

            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) entry.getValue();
            return set != null && set.contains(member);
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== SORTED SET OPERATIONS ====================

    /**
     * ZADD - Add members to sorted set
     */
    public long zadd(String region, String key, double score, String member) {
        Map<String, Double> scoreMembers = new HashMap<>();
        scoreMembers.put(member, score);
        return zadd(region, key, scoreMembers);
    }

    public long zadd(String region, String key, Map<String, Double> scoreMembers) {
        ReadWriteLock lock = getLock(region, key);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getOrCreateEntry(region, key, DataType.SORTED_SET);

            @SuppressWarnings("unchecked")
            TreeSet<SortedSetEntry> sortedSet = (TreeSet<SortedSetEntry>) entry.getValue();
            if (sortedSet == null) {
                sortedSet = new TreeSet<>();
                entry.setValue(sortedSet);
            }

            long added = 0;
            for (Map.Entry<String, Double> scoreMember : scoreMembers.entrySet()) {
                // Remove existing entry with same member
                sortedSet.removeIf(e -> e.getMember().equals(scoreMember.getKey()));

                // Add new entry
                sortedSet.add(new SortedSetEntry(scoreMember.getKey(), scoreMember.getValue()));
                added++;
            }

            saveEntry(entry);
            return added;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * ZRANGE - Get range of members by index
     */
    public List<String> zrange(String region, String key, int start, int stop, boolean withScores) {
        ReadWriteLock lock = getLock(region, key);
        lock.readLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.SORTED_SET);
            if (entry == null) {
                return new ArrayList<>();
            }

            @SuppressWarnings("unchecked")
            TreeSet<SortedSetEntry> sortedSet = (TreeSet<SortedSetEntry>) entry.getValue();
            if (sortedSet == null || sortedSet.isEmpty()) {
                return new ArrayList<>();
            }

            List<SortedSetEntry> entries = new ArrayList<>(sortedSet);
            int size = entries.size();

            int fromIndex = start < 0 ? Math.max(0, size + start) : Math.min(start, size);
            int toIndex = stop < 0 ? Math.max(0, size + stop + 1) : Math.min(stop + 1, size);

            if (fromIndex >= toIndex) {
                return new ArrayList<>();
            }

            List<String> result = new ArrayList<>();
            for (int i = fromIndex; i < toIndex; i++) {
                SortedSetEntry e = entries.get(i);
                result.add(e.getMember());
                if (withScores) {
                    result.add(String.valueOf(e.getScore()));
                }
            }

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * ZREM - Remove members from sorted set
     */
    public long zrem(String region, String key, String... members) {
        ReadWriteLock lock = getLock(region, key);
        lock.writeLock().lock();
        try {
            CacheEntry entry = getEntry(region, key, DataType.SORTED_SET);
            if (entry == null) {
                return 0;
            }

            @SuppressWarnings("unchecked")
            TreeSet<SortedSetEntry> sortedSet = (TreeSet<SortedSetEntry>) entry.getValue();
            if (sortedSet == null) {
                return 0;
            }

            long removed = 0;
            for (String member : members) {
                if (sortedSet.removeIf(e -> e.getMember().equals(member))) {
                    removed++;
                }
            }

            saveEntry(entry);
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ==================== HELPER METHODS ====================

    private CacheEntry getEntry(String region, String key, DataType expectedType) {
        // This would need to integrate with CacheService
        // For now, placeholder implementation
        return null;
    }

    private CacheEntry getOrCreateEntry(String region, String key, DataType dataType) {
        CacheEntry entry = getEntry(region, key, dataType);
        if (entry == null) {
            entry = new CacheEntry(key, region, dataType, null);
        }
        return entry;
    }

    private void saveEntry(CacheEntry entry) {
        cacheRepository.saveEntry(entry);
    }
}