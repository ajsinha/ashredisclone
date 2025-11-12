package com.ash.projects.redisclone.repository;

import com.ash.projects.redisclone.model.CacheEntry;
import jakarta.annotation.PostConstruct;

import java.util.List;

public interface CacheRepositoryInterface {
    @PostConstruct
    void initializeDatabase();

    void saveEntry(CacheEntry entry);

    CacheEntry loadEntry(String region, String key);

    List<CacheEntry> loadAllEntries();

    List<CacheEntry> loadEntriesByRegion(String region);

    void deleteEntry(String region, String key);

    void deleteRegion(String region);

    void updateExpiry(String region, String key, Long expiresAt);

    void deleteExpiredEntries();

    long getEntryCount(String region);
}
