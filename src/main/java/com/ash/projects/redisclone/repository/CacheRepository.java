package com.ash.projects.redisclone.repository;

import com.ash.projects.redisclone.model.CacheEntry;
import com.ash.projects.redisclone.model.DataType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class CacheRepository {

    private static final Logger logger = LoggerFactory.getLogger(CacheRepository.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initializeDatabase() {
        logger.info("Initializing database schema");

        String createTableSql = """
            CREATE TABLE IF NOT EXISTS cache_entries (
                region TEXT NOT NULL,
                key TEXT NOT NULL,
                data_type TEXT NOT NULL,
                value_data TEXT,
                created_at INTEGER NOT NULL,
                last_accessed_at INTEGER NOT NULL,
                expires_at INTEGER,
                in_memory INTEGER DEFAULT 0,
                PRIMARY KEY (region, key)
            )
            """;

        jdbcTemplate.execute(createTableSql);

        // Create indexes
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_region ON cache_entries(region)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_expires_at ON cache_entries(expires_at)");

        logger.info("Database schema initialized");
    }

    public void saveEntry(CacheEntry entry) {
        try {
            String valueJson = objectMapper.writeValueAsString(entry.getValue());

            String sql = """
                INSERT OR REPLACE INTO cache_entries 
                (region, key, data_type, value_data, created_at, last_accessed_at, expires_at, in_memory)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

            jdbcTemplate.update(sql,
                    entry.getRegion(),
                    entry.getKey(),
                    entry.getDataType().name(),
                    valueJson,
                    entry.getCreatedAt(),
                    entry.getLastAccessedAt(),
                    entry.getExpiresAt(),
                    entry.isInMemory() ? 1 : 0
            );

        } catch (Exception e) {
            logger.error("Error saving entry: region={}, key={}", entry.getRegion(), entry.getKey(), e);
        }
    }

    public CacheEntry loadEntry(String region, String key) {
        try {
            String sql = "SELECT * FROM cache_entries WHERE region = ? AND key = ?";

            List<CacheEntry> results = jdbcTemplate.query(sql, new CacheEntryRowMapper(), region, key);

            return results.isEmpty() ? null : results.get(0);

        } catch (Exception e) {
            logger.error("Error loading entry: region={}, key={}", region, key, e);
            return null;
        }
    }

    public List<CacheEntry> loadAllEntries() {
        try {
            String sql = "SELECT * FROM cache_entries";
            return jdbcTemplate.query(sql, new CacheEntryRowMapper());
        } catch (Exception e) {
            logger.error("Error loading all entries", e);
            return List.of();
        }
    }

    public List<CacheEntry> loadEntriesByRegion(String region) {
        try {
            String sql = "SELECT * FROM cache_entries WHERE region = ?";
            return jdbcTemplate.query(sql, new CacheEntryRowMapper(), region);
        } catch (Exception e) {
            logger.error("Error loading entries for region: {}", region, e);
            return List.of();
        }
    }

    public void deleteEntry(String region, String key) {
        try {
            String sql = "DELETE FROM cache_entries WHERE region = ? AND key = ?";
            jdbcTemplate.update(sql, region, key);
        } catch (Exception e) {
            logger.error("Error deleting entry: region={}, key={}", region, key, e);
        }
    }

    public void deleteRegion(String region) {
        try {
            String sql = "DELETE FROM cache_entries WHERE region = ?";
            jdbcTemplate.update(sql, region);
        } catch (Exception e) {
            logger.error("Error deleting region: {}", region, e);
        }
    }

    public void updateExpiry(String region, String key, Long expiresAt) {
        try {
            String sql = "UPDATE cache_entries SET expires_at = ? WHERE region = ? AND key = ?";
            jdbcTemplate.update(sql, expiresAt, region, key);
        } catch (Exception e) {
            logger.error("Error updating expiry: region={}, key={}", region, key, e);
        }
    }

    public void deleteExpiredEntries() {
        try {
            String sql = "DELETE FROM cache_entries WHERE expires_at IS NOT NULL AND expires_at < ?";
            int deleted = jdbcTemplate.update(sql, System.currentTimeMillis());

            if (deleted > 0) {
                logger.info("Deleted {} expired entries from database", deleted);
            }
        } catch (Exception e) {
            logger.error("Error deleting expired entries", e);
        }
    }

    public long getEntryCount(String region) {
        try {
            String sql = "SELECT COUNT(*) FROM cache_entries WHERE region = ?";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, region);
            return count != null ? count : 0;
        } catch (Exception e) {
            logger.error("Error getting entry count for region: {}", region, e);
            return 0;
        }
    }

    private class CacheEntryRowMapper implements RowMapper<CacheEntry> {
        @Override
        public CacheEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            CacheEntry entry = new CacheEntry();
            entry.setRegion(rs.getString("region"));
            entry.setKey(rs.getString("key"));
            entry.setDataType(DataType.valueOf(rs.getString("data_type")));
            entry.setCreatedAt(rs.getLong("created_at"));
            entry.setLastAccessedAt(rs.getLong("last_accessed_at"));

            long expiresAt = rs.getLong("expires_at");
            entry.setExpiresAt(rs.wasNull() ? null : expiresAt);

            entry.setInMemory(rs.getInt("in_memory") == 1);

            // Deserialize value based on data type
            String valueJson = rs.getString("value_data");
            try {
                Object value = deserializeValue(valueJson, entry.getDataType());
                entry.setValue(value);
            } catch (Exception e) {
                logger.error("Error deserializing value for key: {}", entry.getKey(), e);
            }

            return entry;
        }

        private Object deserializeValue(String json, DataType dataType) throws Exception {
            return switch (dataType) {
                case STRING -> objectMapper.readValue(json, String.class);
                case HASH -> objectMapper.readValue(json, objectMapper.getTypeFactory()
                        .constructMapType(java.util.HashMap.class, String.class, String.class));
                case LIST -> objectMapper.readValue(json, objectMapper.getTypeFactory()
                        .constructCollectionType(java.util.ArrayList.class, String.class));
                case SET -> objectMapper.readValue(json, objectMapper.getTypeFactory()
                        .constructCollectionType(java.util.HashSet.class, String.class));
                case SORTED_SET -> objectMapper.readValue(json, objectMapper.getTypeFactory()
                        .constructCollectionType(java.util.TreeSet.class,
                                com.ash.projects.redisclone.model.SortedSetEntry.class));
            };
        }
    }
}