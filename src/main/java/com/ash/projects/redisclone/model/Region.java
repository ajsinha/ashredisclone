package com.ash.projects.redisclone.model;

// Region Model
public class Region {
    private String name;
    private long objectCount;
    private long memoryObjectCount;
    private long dbObjectCount;
    private long createdAt;

    public Region() {
        this.createdAt = System.currentTimeMillis();
    }

    public Region(String name) {
        this();
        this.name = name;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getObjectCount() {
        return objectCount;
    }

    public void setObjectCount(long objectCount) {
        this.objectCount = objectCount;
    }

    public long getMemoryObjectCount() {
        return memoryObjectCount;
    }

    public void setMemoryObjectCount(long memoryObjectCount) {
        this.memoryObjectCount = memoryObjectCount;
    }

    public long getDbObjectCount() {
        return dbObjectCount;
    }

    public void setDbObjectCount(long dbObjectCount) {
        this.dbObjectCount = dbObjectCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
