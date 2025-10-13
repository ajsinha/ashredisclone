package com.ash.projects.redisclone.model;

import java.io.Serializable;

// Base Cache Entry
public class CacheEntry implements Serializable {
    private String key;
    private String region;
    private DataType dataType;
    private Object value;
    private long createdAt;
    private long lastAccessedAt;
    private Long expiresAt;
    private boolean inMemory;

    public CacheEntry() {
        this.createdAt = System.currentTimeMillis();
        this.lastAccessedAt = System.currentTimeMillis();
        this.inMemory = true;
    }

    public CacheEntry(String key, String region, DataType dataType, Object value) {
        this();
        this.key = key;
        this.region = region;
        this.dataType = dataType;
        this.value = value;
    }

    public boolean isExpired() {
        return expiresAt != null && System.currentTimeMillis() > expiresAt;
    }

    public long getTtlSeconds() {
        if (expiresAt == null) {
            return -1;
        }
        long ttl = (expiresAt - System.currentTimeMillis()) / 1000;
        return ttl > 0 ? ttl : -2;
    }

    public void updateAccessTime() {
        this.lastAccessedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public DataType getDataType() { return dataType; }
    public void setDataType(DataType dataType) { this.dataType = dataType; }

    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(long lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }

    public boolean isInMemory() { return inMemory; }
    public void setInMemory(boolean inMemory) { this.inMemory = inMemory; }
}

