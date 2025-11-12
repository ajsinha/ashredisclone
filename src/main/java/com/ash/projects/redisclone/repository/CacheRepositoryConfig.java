package com.ash.projects.redisclone.repository;

import com.ash.projects.redisclone.repository.CacheRepository;
import com.ash.projects.redisclone.repository.CacheRepositoryInterface;
import com.ash.projects.redisclone.repository.CacheRepositoryRocksDB;
import com.ash.projects.redisclone.repository.CacheRepositorySQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration class for Cache Repository.
 * 
 * This class creates the appropriate repository implementation based on the
 * "cache.repository.type" property in application.properties/yaml.
 * 
 * Supported types:
 * - "sql" or "SQL" -> Uses CacheRepositorySQL (SQLite, PostgreSQL, MySQL)
 * - "rocksdb" or "RocksDB" -> Uses CacheRepositoryRocksDB
 * 
 * Default: SQL
 * 
 * The CacheRepository bean is marked as @Primary so it can be injected
 * automatically into services without specifying the implementation type.
 * 
 * Bean Creation Order:
 * 1. First creates the implementation bean (SQL or RocksDB)
 * 2. Then creates the CacheRepository bean wrapping the implementation
 * 3. The @Primary annotation ensures CacheRepository is the default choice
 * 
 * This pattern allows:
 * - Easy switching between SQL and RocksDB by changing configuration
 * - No code changes in services when switching implementations
 * - Single point of dependency injection
 * - Prevents circular dependencies
 * 
 * @author ajsinha@gmail.com
 * Copyright (c) 2025 Ash Sinha. All rights reserved.
 */
@Configuration
public class CacheRepositoryConfig {

    private static final Logger logger = LoggerFactory.getLogger(CacheRepositoryConfig.class);

    @Value("${cache.repository.type:sql}")
    private String repositoryType;

    @Value("${cache.rocksdb.base.path:./data/rocksdb}")
    private String rocksDbBasePath;

    /**
     * Creates the implementation bean based on configuration.
     * This bean will be used internally by the CacheRepository.
     */
    @Bean
    public CacheRepositoryInterface cacheRepositoryImplementation() {
        String type = repositoryType.toLowerCase().trim();
        
        logger.info("Configuring cache repository with type: {}", type);

        CacheRepositoryInterface implementation;
        
        if ("rocksdb".equals(type)) {
            logger.info("Creating RocksDB repository implementation");
            implementation = new CacheRepositoryRocksDB(rocksDbBasePath);
        } else {
            // Default to SQL
            if (!"sql".equals(type)) {
                logger.warn("Unknown repository type '{}', defaulting to SQL", type);
            }
            logger.info("Creating SQL repository implementation");
            implementation = new CacheRepositorySQL();
        }

        return implementation;
    }

    /**
     * Creates the primary CacheRepository bean that delegates to the implementation.
     * 
     * @Primary ensures this bean is injected by default when CacheRepositoryInterface
     * or CacheRepository is autowired.
     */
    @Bean
    @Primary
    public CacheRepository cacheRepository(CacheRepositoryInterface cacheRepositoryImplementation) {
        String implementationType = repositoryType.toLowerCase().trim();
        String typeName = "rocksdb".equals(implementationType) ? "RocksDB" : "SQL";
        
        logger.info("Creating primary CacheRepository bean with {} implementation", typeName);
        
        return new CacheRepository(cacheRepositoryImplementation, typeName);
    }
}
