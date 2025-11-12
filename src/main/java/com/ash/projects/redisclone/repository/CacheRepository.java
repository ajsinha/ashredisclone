package com.ash.projects.redisclone.repository;

import com.ash.projects.redisclone.model.CacheEntry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Delegating implementation of CacheRepositoryInterface.
 * This class delegates all operations to either CacheRepositorySQL or CacheRepositoryRocksDB
 * based on the application configuration.
 * 
 * The actual implementation is injected via constructor and determined by 
 * CacheRepositoryConfig based on the "cache.repository.type" property.
 * 
 * This pattern allows for:
 * 1. Easy switching between SQL and RocksDB implementations
 * 2. Single point of dependency injection for services
 * 3. No code changes required in services when switching implementations
 * 4. Config-driven repository selection
 * 
 * IMPORTANT: This class is NOT annotated with @Repository.
 * It is managed as a bean by CacheRepositoryConfig with @Primary annotation.
 * This prevents circular dependencies and ensures proper bean creation order.
 * 
 * @author ajsinha@gmail.com
 * Copyright (c) 2025 Ash Sinha. All rights reserved.
 */
public class CacheRepository implements CacheRepositoryInterface {

    private static final Logger logger = LoggerFactory.getLogger(CacheRepository.class);
    
    private final CacheRepositoryInterface delegate;
    private final String implementationType;

    /**
     * Constructor with delegate injection
     * 
     * @param delegate The actual repository implementation (SQL or RocksDB)
     * @param implementationType The type name for logging (e.g., "SQL" or "RocksDB")
     */
    public CacheRepository(CacheRepositoryInterface delegate, String implementationType) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate repository cannot be null");
        }
        this.delegate = delegate;
        this.implementationType = implementationType;
        logger.info("CacheRepository initialized with {} implementation", implementationType);
    }

    @PostConstruct
    @Override
    public void initializeDatabase() {
        logger.info("Initializing {} cache repository", implementationType);
        delegate.initializeDatabase();
        logger.info("{} cache repository initialized successfully", implementationType);
    }

    @Override
    public void saveEntry(CacheEntry entry) {
        delegate.saveEntry(entry);
    }

    @Override
    public CacheEntry loadEntry(String region, String key) {
        return delegate.loadEntry(region, key);
    }

    @Override
    public List<CacheEntry> loadAllEntries() {
        return delegate.loadAllEntries();
    }

    @Override
    public List<CacheEntry> loadEntriesByRegion(String region) {
        return delegate.loadEntriesByRegion(region);
    }

    @Override
    public void deleteEntry(String region, String key) {
        delegate.deleteEntry(region, key);
    }

    @Override
    public void deleteRegion(String region) {
        delegate.deleteRegion(region);
    }

    @Override
    public void updateExpiry(String region, String key, Long expiresAt) {
        delegate.updateExpiry(region, key, expiresAt);
    }

    @Override
    public void deleteExpiredEntries() {
        delegate.deleteExpiredEntries();
    }

    @Override
    public long getEntryCount(String region) {
        return delegate.getEntryCount(region);
    }

    /**
     * Get the underlying delegate (for advanced use cases)
     * This allows access to implementation-specific methods
     * 
     * @return The underlying repository implementation
     */
    public CacheRepositoryInterface getDelegate() {
        return delegate;
    }

    /**
     * Get the type of repository implementation
     * 
     * @return "SQL" or "RocksDB"
     */
    public String getImplementationType() {
        return implementationType;
    }

    /**
     * Check if the current implementation is SQL-based
     * 
     * @return true if using SQL implementation
     */
    public boolean isSqlImplementation() {
        return delegate instanceof CacheRepositorySQL;
    }

    /**
     * Check if the current implementation is RocksDB-based
     * 
     * @return true if using RocksDB implementation
     */
    public boolean isRocksDbImplementation() {
        return delegate instanceof CacheRepositoryRocksDB;
    }

    /**
     * Get the SQL repository if available (for migration scenarios)
     * 
     * @return CacheRepositorySQL instance or null
     */
    public CacheRepositorySQL getSqlRepository() {
        if (delegate instanceof CacheRepositorySQL) {
            return (CacheRepositorySQL) delegate;
        }
        return null;
    }

    /**
     * Get the RocksDB repository if available (for advanced features)
     * 
     * @return CacheRepositoryRocksDB instance or null
     */
    public CacheRepositoryRocksDB getRocksDbRepository() {
        if (delegate instanceof CacheRepositoryRocksDB) {
            return (CacheRepositoryRocksDB) delegate;
        }
        return null;
    }
}
