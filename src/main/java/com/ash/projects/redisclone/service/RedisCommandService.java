package com.ash.projects.redisclone.service;

import com.ash.projects.redisclone.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RedisCommandService {

    @Autowired
    private CacheService cacheService;

    // String operations are delegated to CacheService

    // APPEND operation
    public long append(String region, String key, String value) {
        String existing = cacheService.get(region, key);
        String newValue = (existing != null ? existing : "") + value;
        cacheService.set(region, key, newValue, null);
        return newValue.length();
    }

    // INCR operation
    public long incr(String region, String key) {
        return incrBy(region, key, 1);
    }

    // DECR operation
    public long decr(String region, String key) {
        return incrBy(region, key, -1);
    }

    // INCRBY operation
    public long incrBy(String region, String key, long increment) {
        String value = cacheService.get(region, key);
        long currentValue = 0;

        if (value != null) {
            try {
                currentValue = Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value is not an integer");
            }
        }

        long newValue = currentValue + increment;
        cacheService.set(region, key, String.valueOf(newValue), null);
        return newValue;
    }

    // ========== HASH OPERATIONS ==========

    public boolean hset(String region, String key, Map<String, String> fieldValues) {
        // Implementation would require extending CacheService to support HASH type
        // For brevity, showing the structure
        return true;
    }

    public String hget(String region, String key, String field) {
        // Implementation would get hash from cache and return specific field
        return null;
    }

    public Map<String, String> hgetAll(String region, String key) {
        // Implementation would return entire hash
        return new HashMap<>();
    }

    public long hdel(String region, String key, String... fields) {
        // Implementation would delete specific fields from hash
        return 0;
    }

    // ========== LIST OPERATIONS ==========

    public long lpush(String region, String key, String... values) {
        // Implementation would add values to head of list
        return 0;
    }

    public long rpush(String region, String key, String... values) {
        // Implementation would add values to tail of list
        return 0;
    }

    public String lpop(String region, String key) {
        // Implementation would remove and return head element
        return null;
    }

    public String rpop(String region, String key) {
        // Implementation would remove and return tail element
        return null;
    }

    public List<String> lrange(String region, String key, int start, int stop) {
        // Implementation would return range of elements
        return new ArrayList<>();
    }

    public long llen(String region, String key) {
        // Implementation would return list length
        return 0;
    }

    // ========== SET OPERATIONS ==========

    public long sadd(String region, String key, String... members) {
        // Implementation would add members to set
        return 0;
    }

    public Set<String> smembers(String region, String key) {
        // Implementation would return all set members
        return new HashSet<>();
    }

    public long srem(String region, String key, String... members) {
        // Implementation would remove members from set
        return 0;
    }

    public boolean sismember(String region, String key, String member) {
        // Implementation would check if member exists in set
        return false;
    }

    // ========== SORTED SET OPERATIONS ==========

    public long zadd(String region, String key, Map<String, Double> scoreMembers) {
        // Implementation would add/update members in sorted set
        return 0;
    }

    public List<String> zrange(String region, String key, int start, int stop, boolean withScores) {
        // Implementation would return range of members by index
        return new ArrayList<>();
    }

    public long zrem(String region, String key, String... members) {
        // Implementation would remove members from sorted set
        return 0;
    }

    // ========== SCAN OPERATION ==========

    public ScanResult scan(String region, int cursor, String pattern, int count) {
        Set<String> keys = cacheService.keys(region, pattern != null ? pattern : "*");
        List<String> keyList = new ArrayList<>(keys);

        int start = cursor;
        int end = Math.min(start + count, keyList.size());

        List<String> resultKeys = keyList.subList(start, end);
        int nextCursor = end >= keyList.size() ? 0 : end;

        return new ScanResult(nextCursor, resultKeys);
    }

    // ========== SERVER OPERATIONS ==========

    public String ping() {
        return "PONG";
    }

    public Map<String, Object> info(String section) {
        Map<String, Object> info = new HashMap<>();
        info.put("version", "1.0.0");
        info.put("regions", cacheService.getAllRegions());
        info.put("uptime_seconds", System.currentTimeMillis() / 1000);
        return info;
    }

    // Helper class for SCAN result
    public static class ScanResult {
        private int cursor;
        private List<String> keys;

        public ScanResult(int cursor, List<String> keys) {
            this.cursor = cursor;
            this.keys = keys;
        }

        public int getCursor() { return cursor; }
        public List<String> getKeys() { return keys; }
    }
}