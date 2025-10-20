# AshRedis Clone - Quick Start Guide

## © 2025-2030 Ashutosh Sinha

Get up and running with AshRedis in minutes!

## Prerequisites

- Java 17+ installed
- Maven 3.6+ installed
- (Optional) Docker for containerized deployment
- (Optional) Kafka for clustering/replication

**Note:** By default, AshRedis runs as a **standalone PRIMARY instance** without requiring Kafka or clustering.

## 5-Minute Quick Start

### Step 1: Build the Project

```bash
# Clone the repository (if applicable)
cd ashredisclone

# Build the project
mvn clean package

# This creates: target/redisclone-1.0.0.jar
```

### Step 2: Start the Server

```bash
# Start with default settings (standalone PRIMARY mode)
java -jar target/redisclone-1.0.0.jar
```

That's it! The server is now running with:
- **Mode:** Standalone PRIMARY (clustering disabled)
- **Web UI:** http://localhost:8080
- **Network Server:** localhost:6379
- **All operations enabled** (read and write)

**Important:** By default, clustering is **disabled** and the instance runs as PRIMARY. This means:
- ✅ Single instance is fully functional
- ✅ No Kafka required
- ✅ Perfect for development and testing
- ✅ All write operations work immediately

### Step 3: Access the Web UI

1. Open browser: http://localhost:8080
2. Login with: `admin` / `admin123`
3. Explore the dashboard!

### Step 4: Test with Client

**Using Java Client:**
```java
AshRedisClient client = new AshRedisClient("localhost", 6379);
client.connect();
client.set("test", "Hello World!");
System.out.println(client.get("test"));
client.close();
```

**Using Python Client:**
```python
from ashredis_client import AshRedisClient
with AshRedisClient('localhost', 6379) as client:
    client.set('test', 'Hello World!')
    print(client.get('test'))
```

**Using netcat:**
```bash
echo "PING" | nc localhost 6379
echo "SET @myregion mykey myvalue" | nc localhost 6379
echo "GET @myregion mykey" | nc localhost 6379
```

## Running with Docker

### Option 1: Single Instance

```bash
# Build image
docker build -t ashredis:latest .

# Run container
docker run -d \
  -p 8080:8080 \
  -p 6379:6379 \
  --name ashredis \
  ashredis:latest
```

### Option 2: Complete Cluster with Kafka

```bash
# Start everything (includes Kafka, ActiveMQ, Primary, Secondary)
docker-compose up -d

# View logs
docker-compose logs -f

# Access:
# - Primary Web UI: http://localhost:8080
# - Secondary Web UI: http://localhost:8081
# - Primary Network: localhost:6379
# - Secondary Network: localhost:6380
```

## Running Primary-Secondary Cluster

### Without Docker (Manual)

**Terminal 1 - Start Kafka:**
```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Terminal 2 - Start Kafka
bin/kafka-server-start.sh config/server.properties
```

**Terminal 3 - Start Primary:**
```bash
java -jar target/redisclone-1.0.0.jar \
  --cluster.mode=primary \
  --server.port=8080 \
  --network.server.port=6379
```

**Terminal 4 - Start Secondary:**
```bash
java -jar target/redisclone-1.0.0.jar \
  --cluster.mode=secondary \
  --server.port=8081 \
  --network.server.port=6380
```

### With Helper Scripts

```bash
# Make scripts executable
chmod +x start.sh run-cluster.sh stop-cluster.sh

# Start complete cluster
./run-cluster.sh

# Test connection
./test-connection.sh

# Stop cluster
./stop-cluster.sh
```

## Common Configuration Overrides

### Increase Memory Limit
```bash
java -jar target/redisclone-1.0.0.jar \
  --cache.max.memory.objects=500000
```

### Use Different Database
```bash
java -jar target/redisclone-1.0.0.jar \
  --spring.datasource.url=jdbc:sqlite:/data/myredis.db
```

### Disable Replication
```bash
java -jar target/redisclone-1.0.0.jar \
  --kafka.replication.enabled=false \
  --cluster.enabled=false
```

### Custom Ports
```bash
java -jar target/redisclone-1.0.0.jar \
  --server.port=9090 \
  --network.server.port=7000
```

## Testing Commands

### Via Network Protocol
```bash
# PING
echo "PING" | nc localhost 6379

# SET with region
echo "SET @users user:1 John" | nc localhost 6379

# GET with region
echo "GET @users user:1" | nc localhost 6379

# SET with TTL (60 seconds)
echo "SET mykey myvalue EX 60" | nc localhost 6379

# DEL
echo "DEL mykey" | nc localhost 6379

# KEYS pattern
echo "KEYS user:*" | nc localhost 6379

# INCR
echo "INCR counter" | nc localhost 6379
```

### Via Java Client
```java
try (AshRedisClient client = new AshRedisClient("localhost", 6379)) {
    client.connect();
    
    // Use specific region
    client.useRegion("users");
    
    // Operations
    client.set("user:1", "John Doe");
    client.set("user:2", "Jane Smith");
    client.set("user:3", "Bob Wilson");
    
    // Pattern search
    Set<String> users = client.keys("user:*");
    System.out.println("Found users: " + users);
    
    // With TTL
    client.set("session:abc", "data", 3600L);
    
    // Check TTL
    long ttl = client.ttl("session:abc");
    System.out.println("TTL: " + ttl + " seconds");
}
```

### Via Python Client
```python
from ashredis_client import AshRedisClient

with AshRedisClient('localhost', 6379) as client:
    # Use specific region
    client.use_region('products')
    
    # Operations
    client.set('product:1', 'Laptop')
    client.set('product:2', 'Mouse')
    client.set('product:3', 'Keyboard')
    
    # Pattern search
    products = client.keys('product:*')
    print(f"Found products: {products}")
    
    # Increment counter
    views = client.incr('page:views')
    print(f"Page views: {views}")
```

## Web UI Features

### Dashboard
- View instance status (Primary/Secondary)
- Browse available regions
- System information

### Regions
- List all regions with statistics
- Click to explore keys in region
- Search with patterns (e.g., `user:*`, `session:*`)

### Entry Management
- View entry details (key, value, TTL)
- Create new entries (Admin only)
- Update values and TTL (Admin only)
- Delete entries (Admin only)

### Statistics
- System version and uptime
- Region counts
- Pub/Sub subscription counts

## Advanced Features

### PUBTODEST - Async Publishing

**Publish to File:**
```bash
# Via network
echo "PUBTODEST @users user:* file:///tmp/users.json" | nc localhost 6379
# Returns: UUID for tracking
```

**Publish to Kafka:**
```bash
echo "PUBTODEST @logs log:* kafka://async/topic/logs" | nc localhost 6379
```

**Publish to ActiveMQ:**
```bash
echo "PUBTODEST @events event:* activemq://default/queue/events" | nc localhost 6379
```

### Transactions

```bash
# Start transaction
echo "MULTI" | nc localhost 6379

# Queue commands
echo "SET key1 value1" | nc localhost 6379
echo "SET key2 value2" | nc localhost 6379
echo "INCR counter" | nc localhost 6379

# Execute all
echo "EXEC" | nc localhost 6379
```

### Pub/Sub

**Publisher:**
```java
client.publish("news", "Breaking news!");
```

**Subscriber:**
```java
client.subscribe("news", message -> {
    System.out.println("Received: " + message);
});
```

## Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### View Logs
```bash
# If running with run-cluster.sh
tail -f logs/primary.log
tail -f logs/secondary.log

# Docker
docker-compose logs -f ashredis-primary
docker-compose logs -f ashredis-secondary
```

### Check Replication
1. Set value on primary
2. Check it appears on secondary
3. Try to write on secondary (should fail)

```bash
# On primary (port 6379)
echo "SET testkey testvalue" | nc localhost 6379

# On secondary (port 6380) - should work
echo "GET testkey" | nc localhost 6380

# On secondary - should fail
echo "SET testkey2 value" | nc localhost 6380
```

## Troubleshooting

### Port Already in Use
```bash
# Check what's using port 8080
lsof -i :8080
# or
netstat -an | grep 8080

# Use different port
java -jar target/redisclone-1.0.0.jar --server.port=9090
```

### Can't Connect to Kafka
- Ensure Kafka is running: `docker-compose ps`
- Check Kafka URL in application.properties
- Disable replication: `--kafka.replication.enabled=false`

### Database Errors
- Check disk space
- Verify write permissions in application directory
- Delete and recreate: `rm ashredis.db`

### Login Issues
- Default credentials: `admin` / `admin123`
- Check `users.json` file exists
- Verify JSON format is valid

## Next Steps

- Read the full [README.md](README.md) for detailed documentation
- Explore the Web UI features
- Integrate with your application using Java or Python clients
- Set up production cluster with Kafka replication
- Configure custom regions for your use case

## Getting Help

Check logs for detailed error messages:
```bash
# Console output
java -jar target/redisclone-1.0.0.jar

# Or with log file
java -jar target/redisclone-1.0.0.jar > app.log 2>&1
```

## Quick Reference

| Feature | Default Value |
|---------|--------------|
| Web UI Port | 8080 |
| Network Port | 6379 |
| Default Region | region0 |
| Max Memory Objects | 100,000 |
| Database | SQLite (ashredis.db) |
| Default User | admin/admin123 |

Happy caching with AshRedis! 🚀

## Support

For issues, questions, or contributions, please contact:
- Email: ajsinha@gmail.com
- GitHub: https://github.com/yourusername/sajhamcpserver

## License

© 2025-2030 Ashutosh Sinha. All rights reserved.

This software is proprietary and confidential. Unauthorized copying, distribution, or use is strictly prohibited.

