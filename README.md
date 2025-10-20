# AshRedis Clone - High Performance Redis Implementation in Java

## © 2025-2030 Ashutosh Sinha

A feature-rich, high-performance Redis clone implemented in Java 17 with Spring Boot, supporting network accessibility, distributed caching, primary-secondary replication, and a comprehensive web management UI.

## Features

### Core Functionality
- **In-Memory Caching** with configurable LRU eviction (default: 100,000 objects)
- **Database Persistence** using SQLite for overflow storage
- **Region-Based Partitioning** (unique to this implementation)
- **TTL Support** with automated background cleanup
- **Auto-Priming** from database on restart

### Data Types Supported
- Strings
- Hashes
- Lists
- Sets
- Sorted Sets

### Redis Commands
Comprehensive support for Redis commands including:
- Key operations: `SET`, `GET`, `DEL`, `EXISTS`, `EXPIRE`, `TTL`, `PERSIST`, `KEYS`, `SCAN`
- String operations: `APPEND`, `INCR`, `DECR`
- Hash operations: `HSET`, `HGET`, `HGETALL`, `HDEL`
- List operations: `LPUSH`, `RPUSH`, `LPOP`, `RPOP`, `LRANGE`, `LLEN`
- Set operations: `SADD`, `SMEMBERS`, `SREM`, `SISMEMBER`
- Sorted Set operations: `ZADD`, `ZRANGE`, `ZREM`
- Pub/Sub: `PUBLISH`, `SUBSCRIBE`
- Transactions: `MULTI`, `EXEC`, `DISCARD`
- Server: `PING`, `INFO`, `CLIENT LIST`

### Network & Clients
- **Apache Mina** based network server (default port: 6379)
- **Java Client Library** for easy integration
- **Python Client Library** for cross-platform access
- **Pub/Sub Support** with channel, region, and key-based subscriptions

### Advanced Features
- **Primary-Secondary Replication** via Kafka with automatic failover
- **Async Pub (PUBTODEST)** to publish cache data to:
    - File systems
    - Kafka topics
    - ActiveMQ queues/topics
- **Health Monitoring** and automatic failover using Kafka ephemeral topics

### Web Management UI
- Bootstrap and jQuery based responsive UI
- Role-based access control (Admin, User, Viewer)
- Features:
    - **Dashboard**: System overview and quick actions
    - **Region Management**: Create, view, and delete regions
    - **Entry Management**: Create, update, delete entries with TTL control
    - **Advanced Search**: Search by region and key (exact, contains, or regex)
    - **Query with regex patterns**: Flexible pattern matching
    - **Statistics**: View system and Pub/Sub stats
    - Read-only mode on secondary instances

## Project Structure

```
com.ash.projects.redisclone/
├── model/              # Domain models
├── service/            # Business logic
│   ├── CacheService
│   ├── ExtendedCacheService
│   ├── RedisCommandService
│   ├── ReplicationService
│   ├── PubSubService
│   ├── AsyncPubService
│   ├── TransactionService
│   └── UserService
├── repository/         # Database layer
├── controller/         # Web controllers
├── network/            # Network server (Mina)
└── client/             # Java client library
```

## Building the Project

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Kafka (optional, for clustering/replication)
- ActiveMQ (optional, for async pub)

**Note:** AshRedis works perfectly as a standalone instance without Kafka or ActiveMQ. Clustering is disabled by default.

### Build Fat JAR

```bash
mvn clean package
```

The fat JAR will be created at: `target/redisclone-1.0.0.jar`

## Configuration

Edit `application.properties`:

### Cache Configuration
```properties
cache.max.memory.objects=100000
cache.eviction.policy=LRU
cache.default.region=region0
cache.cleanup.interval.seconds=60
```

### Database Configuration
```properties
spring.datasource.url=jdbc:sqlite:ashredis.db
spring.datasource.driver-class-name=org.sqlite.JDBC
cache.database.enabled=true
```

### Network Server
```properties
network.server.enabled=true
network.server.port=6379
network.server.bind.address=0.0.0.0
```

### Kafka Replication
```properties
kafka.replication.enabled=true
spring.kafka.bootstrap-servers=localhost:9092
kafka.replication.topic=ashredis-replication
kafka.health.topic=ashredis-health
```

### Clustering
```properties
cluster.enabled=true
cluster.mode=primary
cluster.health.check.interval.ms=5000
cluster.failover.timeout.ms=15000
```

### Web UI
```properties
server.port=8080
web.ui.enabled=true
```

## Running the Application

### Start Single Instance (Default - Recommended)
```bash
java -jar target/redisclone-1.0.0.jar
```

**Default Behavior:**
- Runs as **PRIMARY** instance
- Clustering is **disabled**
- All operations work immediately
- No Kafka required
- Perfect for development and production single-instance deployments

### Start Primary Instance (Clustered Mode)
```bash
java -jar target/redisclone-1.0.0.jar \
  --cluster.enabled=true \
  --cluster.mode=primary \
  --kafka.replication.enabled=true
```

### Start Secondary Instance (Clustered Mode)
```bash
java -jar target/redisclone-1.0.0.jar \
  --cluster.enabled=true \
  --cluster.mode=secondary \
  --server.port=8081 \
  --network.server.port=6380 \
  --kafka.replication.enabled=true
```

**Auto-Promotion:** If a secondary starts without detecting a primary within 10 seconds, it automatically promotes itself to primary.

## User Management

Users are configured in `src/main/resources/users.json`:

```json
{
  "users": [
    {
      "userid": "admin",
      "name": "Administrator",
      "password": "admin123",
      "roles": ["ADMIN", "USER"]
    }
  ]
}
```

### Roles
- **ADMIN**: Full access including create, update, delete
- **USER**: Read and query access
- **VIEWER**: Read-only access

## Using the Java Client

```java
try (AshRedisClient client = new AshRedisClient("localhost", 6379)) {
    client.connect();
    
    // Basic operations
    client.set("mykey", "myvalue");
    String value = client.get("mykey");
    
    // With region
    client.set("users", "user:1", "John Doe");
    
    // With TTL
    client.set("session", "session:123", "data", 3600L);
    
    // Pattern matching
    Set<String> keys = client.keys("user:*");
    
    // Increment
    long counter = client.incr("counter");
}
```

## Using the Python Client

```python
from ashredis_client import AshRedisClient

with AshRedisClient('localhost', 6379) as client:
    # Basic operations
    client.set('mykey', 'myvalue')
    value = client.get('mykey')
    
    # With region
    client.set('user:1', 'John Doe', region='users')
    
    # With TTL
    client.set('session:123', 'data', ttl_seconds=3600)
    
    # Pattern matching
    keys = client.keys('user:*')
    
    # Increment
    counter = client.incr('counter')
```

## Web UI Access

1. Open browser: `http://localhost:8080`
2. Login with credentials from `users.json`
3. Default admin: `admin` / `admin123`

### Features
- **Dashboard**: System overview and status
- **Regions**: Browse and manage cache regions
- **Entries**: View, create, update, delete cache entries
- **Statistics**: System and Pub/Sub statistics

## PUBTODEST Command

Asynchronously publish cache entries to external destinations:

```java
// Via network protocol
PUBTODEST @region user:* file:///tmp/users.json

// To Kafka topic
PUBTODEST @region session:* kafka://async/topic/sessions

// To ActiveMQ queue
PUBTODEST @region logs:* activemq://default/queue/cache-logs

// Returns UUID for tracking
```

Track publication status via Web UI or API.

## Primary-Secondary Replication

### How It Works
1. Primary instance handles all write operations
2. Changes are published to Kafka replication topic
3. Secondary instances consume and apply changes
4. Health checks via Kafka ephemeral topics
5. Automatic failover if primary fails (15s timeout)

### Failover Process
1. Secondary detects primary heartbeat timeout
2. Election based on instance ID (lexicographic order)
3. Winner promotes itself to primary
4. New primary starts handling writes

## Architecture Highlights

### LRU Eviction
- Memory objects tracked per region
- Oldest entries evicted to database when limit exceeded
- Keys always remain in memory for fast lookups

### Region Partitioning
- Each object belongs to a region (default: `region0`)
- Keys unique within region, not globally
- Enables logical data separation

### Database Schema
```sql
CREATE TABLE cache_entries (
    region TEXT NOT NULL,
    key TEXT NOT NULL,
    data_type TEXT NOT NULL,
    value_data TEXT,
    created_at INTEGER NOT NULL,
    last_accessed_at INTEGER NOT NULL,
    expires_at INTEGER,
    in_memory INTEGER DEFAULT 0,
    PRIMARY KEY (region, key)
);
```

## Performance Considerations

- Concurrent access via ReadWriteLocks
- LRU implemented with LinkedHashMap per region
- Database writes are asynchronous where possible
- Network layer uses Apache Mina NIO for scalability

## Monitoring & Health Checks

### Endpoints
- Web UI: `http://localhost:8080`
- Network: `tcp://localhost:6379`
- Health: Via Kafka health topic

### Metrics
- Total regions
- Keys per region
- Memory vs database objects
- Pub/Sub subscription counts
- Replication status

## Troubleshooting

### Connection Issues
- Verify network.server.port is not in use
- Check firewall settings
- Ensure Kafka/ActiveMQ are running if enabled

### Replication Issues
- Check Kafka connectivity
- Verify replication topic exists
- Review logs for replication events

### Performance Issues
- Increase cache.max.memory.objects
- Tune cleanup.interval.seconds
- Check database disk I/O
- Review LRU eviction logs

## Development

### Adding New Commands
1. Add method to `RedisCommandService`
2. Add protocol handler in `NetworkServer`
3. Update clients (Java/Python)
4. Add tests

### Extending Data Types
1. Add type to `DataType` enum
2. Implement operations in `ExtendedCacheService`
3. Update serialization in `CacheRepository`

## License

Proprietary - AshRedis Clone © 2025

## Support

For issues, questions, or contributions, please contact:
- Email: ajsinha@gmail.com
- GitHub: https://github.com/yourusername/sajhamcpserver

## License

© 2025-2030 Ashutosh Sinha. All rights reserved.

This software is proprietary and confidential. Unauthorized copying, distribution, or use is strictly prohibited.

