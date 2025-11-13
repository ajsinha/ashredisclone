# AshRedis Backend Service and Features

**Version:** 3.0  
**Copyright:** All Rights Reserved 2025-2030, Ashutosh Sinha  
**Author:** ajsinha@gmail.com  
**Project:** Abhikarta LLM Platform - Redis Clone

---

## Legal Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

This software is intended for educational and development purposes. Users are responsible for ensuring compliance with all applicable laws and regulations in their jurisdiction. The author assumes no liability for any misuse, data loss, or security breaches resulting from the use of this software.

By using this software, you agree to:
- Use it in compliance with all applicable laws
- Not hold the author liable for any damages
- Accept that the software is provided without warranty
- Take full responsibility for your deployment and usage

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Core Features](#core-features)
4. [Data Structures](#data-structures)
5. [Storage Backends](#storage-backends)
6. [Network Protocol](#network-protocol)
7. [Region Management](#region-management)
8. [Replication and High Availability](#replication-and-high-availability)
9. [Pub/Sub Messaging](#pubsub-messaging)
10. [Asynchronous Publishing](#asynchronous-publishing)
11. [Transaction Support](#transaction-support)
12. [Security and Authentication](#security-and-authentication)
13. [REST API](#rest-api)
14. [Client Libraries](#client-libraries)
15. [Configuration Reference](#configuration-reference)
16. [Deployment Guide](#deployment-guide)
17. [Monitoring and Operations](#monitoring-and-operations)
18. [Performance Tuning](#performance-tuning)
19. [Advanced Features](#advanced-features)
20. [Troubleshooting](#troubleshooting)

---

## Overview

AshRedis is a feature-rich, production-ready Redis-compatible cache service built with Spring Boot and Java. It provides a comprehensive key-value store with advanced capabilities including multi-region support, replication, pub/sub messaging, transactions, and multiple storage backends.

### Key Highlights

✅ **Redis Protocol Compatible** - Works with standard Redis clients  
✅ **Multi-Region Support** - Logical partitioning of data  
✅ **Dual Storage Backends** - SQL (SQLite, PostgreSQL, MySQL) or RocksDB  
✅ **High Availability** - Kafka-based replication with automatic failover  
✅ **Pub/Sub Messaging** - Channel-based and region-based subscriptions  
✅ **Transaction Support** - MULTI/EXEC/DISCARD commands  
✅ **Security** - User authentication and role-based access control  
✅ **REST API** - HTTP interface alongside Redis protocol  
✅ **Memory Management** - Heap-based auto-eviction, LRU eviction  
✅ **Production Ready** - Comprehensive logging, monitoring, error handling  

### Use Cases

- **Microservices Cache** - Shared cache layer for distributed systems
- **Session Store** - High-performance session management
- **Message Queue** - Pub/Sub based messaging between services
- **Rate Limiting** - Fast counter and expiration support
- **Multi-Tenant Systems** - Region-based data isolation
- **Edge Computing** - Lightweight deployment with embedded database
- **Development/Testing** - Redis-compatible mock for testing

---

## Architecture

### System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        AshRedis System                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              Network Layer (Port 6379)                     │ │
│  │  ┌──────────────────────────────────────────────────────┐ │ │
│  │  │  Apache MINA NIO Server                              │ │ │
│  │  │  - Redis Protocol Handler                            │ │ │
│  │  │  - Enhanced Command Parser                           │ │ │
│  │  │  - Session Management                                │ │ │
│  │  └──────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────┘ │
│                             ↓                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              REST API Layer (Port 8080)                    │ │
│  │  ┌──────────────────────────────────────────────────────┐ │ │
│  │  │  Spring MVC Controllers                              │ │ │
│  │  │  - WebController (HTTP endpoints)                    │ │ │
│  │  │  - Security Filters                                  │ │ │
│  │  └──────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────┘ │
│                             ↓                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              Service Layer                                 │ │
│  │  ┌──────────────┬──────────────┬──────────────────────┐  │ │
│  │  │ CacheService │ PubSubService│ ReplicationService   │  │ │
│  │  ├──────────────┼──────────────┼──────────────────────┤  │ │
│  │  │ Transaction  │ AsyncPub     │ RedisCommandService  │  │ │
│  │  │ Service      │ Service      │                      │  │ │
│  │  └──────────────┴──────────────┴──────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────┘ │
│                             ↓                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              Memory Cache Layer                            │ │
│  │  ┌──────────────────────────────────────────────────────┐ │ │
│  │  │  - Region-based partitioning                         │ │ │
│  │  │  - LRU eviction                                      │ │ │
│  │  │  - Heap-based auto-eviction                          │ │ │
│  │  │  - Thread-safe operations                            │ │ │
│  │  └──────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────┘ │
│                             ↓                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              Persistence Layer                             │ │
│  │  ┌──────────────────────┬───────────────────────────────┐ │ │
│  │  │  SQL Repository      │    RocksDB Repository         │ │ │
│  │  │  - SQLite (default)  │    - Embedded key-value store │ │ │
│  │  │  - PostgreSQL        │    - High performance         │ │ │
│  │  │  - MySQL             │    - Low latency              │ │ │
│  │  └──────────────────────┴───────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────┘ │
│                             ↓                                    │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              External Integrations                         │ │
│  │  ┌──────────────────────────────────────────────────────┐ │ │
│  │  │  - Kafka (Replication & Messaging)                   │ │ │
│  │  │  - ActiveMQ (Async Publishing)                       │ │ │
│  │  └──────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Component Interactions

```
Redis Client              REST Client
    │                         │
    ├─────► Network Server    │
    │       (Port 6379)       │
    │            │            │
    │            └────────────┼─────► WebController
    │                         │       (Port 8080)
    │                         │            │
    └─────────────────────────┼────────────┘
                              ↓
                      CacheService (Core)
                              │
                    ┌─────────┼─────────┐
                    ↓         ↓         ↓
            ReplicationSvc PubSubSvc  TransactionSvc
                    ↓         ↓         ↓
              Memory Cache Layer
                    │         │         │
                    └─────────┼─────────┘
                              ↓
                   Persistence Layer
                    (SQL or RocksDB)
```

---

## Core Features

### 1. Fair Region Distribution

**Problem Solved:** Equal memory allocation across all regions during initialization.

**How It Works:**
- Round-robin loading algorithm
- Each region gets equal share of memory
- Prevents first region from monopolizing cache

**Configuration:**
```properties
cache.max.memory.objects=100000
```

**Example:**
```
3 regions with 100K memory slots:
- region1: 33,333 entries
- region2: 33,333 entries
- region3: 33,334 entries
```

---

### 2. Heap-Based Auto-Eviction

**Problem Solved:** Prevents OutOfMemoryError crashes under high load.

**How It Works:**
- Monitors JVM heap every 30 seconds (configurable)
- When heap exceeds threshold (default 80%), evicts LRU entries
- Saves evicted entries to disk
- Hints garbage collection

**Configuration:**
```properties
# Enable heap monitoring
cache.heap.monitor.enabled=true

# Threshold percentage
cache.heap.threshold.percent=80

# Batch size per eviction
cache.heap.eviction.batch.size=1000

# Monitoring interval
cache.heap.monitor.interval.ms=30000
```

**Benefits:**
- Self-healing under memory pressure
- Maintains application availability
- Automatic and transparent
- Configurable for different scenarios

---

### 3. Shutdown Persistence

**Problem Solved:** Zero data loss during graceful shutdowns.

**How It Works:**
- Persists all in-memory entries to database before shutdown
- Marks entries as not in memory
- Clears memory structures
- Comprehensive logging

**Result:**
- Safe restarts and deployments
- Consistent state between memory and database

---

### 4. LRU Eviction

**How It Works:**
- Tracks access time for each entry
- Evicts least recently used entries when memory full
- Per-region LRU tracking
- Transparent to application

---

### 5. Enhanced Command Parser

**Features:**
- Handles single and double quotes
- Supports escaped characters: `\"`, `\'`, `\\`
- Supports escape sequences: `\n`, `\r`, `\t`
- Preserves whitespace within quotes
- Handles JSON payloads
- Validates quote closure

**Examples:**
```bash
SET key "Hello World"
SET key "{\"name\": \"John\"}"
SET key "He said \"Hi\""
```

---

## Data Structures

### String Operations

Basic key-value operations.

**Commands:**
```bash
SET key value [EX seconds]     # Set value with optional expiration
GET key                         # Get value
DEL key [key ...]              # Delete keys
EXISTS key [key ...]           # Check if keys exist
APPEND key value               # Append to string
INCR key                       # Increment by 1
DECR key                       # Decrement by 1
```

**Java Example:**
```java
cacheService.set(region, "counter", "0", null);
cacheService.incr(region, "counter");
String value = cacheService.get(region, "counter"); // "1"
```

**Python Example:**
```python
client.set('counter', '0')
client.incr('counter')
value = client.get('counter')  # '1'
```

---

### Hash Operations

Store multiple field-value pairs under a single key.

**Commands:**
```bash
HSET key field value          # Set hash field
HGET key field                # Get hash field
HGETALL key                   # Get all fields
HDEL key field [field ...]    # Delete fields
```

**Use Cases:**
- User profiles
- Object storage
- Configuration data

---

### List Operations

Ordered collections with head/tail operations.

**Commands:**
```bash
LPUSH key value [value ...]   # Push to head
RPUSH key value [value ...]   # Push to tail
LPOP key                      # Pop from head
RPOP key                      # Pop from tail
LRANGE key start stop         # Get range
LLEN key                      # Get length
```

**Use Cases:**
- Message queues
- Activity streams
- Task lists

---

### Set Operations

Unordered unique collections.

**Commands:**
```bash
SADD key member [member ...]  # Add members
SMEMBERS key                  # Get all members
SREM key member [member ...]  # Remove members
SISMEMBER key member          # Check membership
```

**Use Cases:**
- Tags
- Unique visitors
- Social connections

---

### Sorted Set Operations

Ordered collections with scores.

**Commands:**
```bash
ZADD key score member         # Add with score
ZRANGE key start stop         # Get range by index
ZREM key member [member ...]  # Remove members
```

**Use Cases:**
- Leaderboards
- Priority queues
- Time series data

---

## Storage Backends

### SQL Backend (Default)

**Supported Databases:**
- SQLite (embedded, no installation required)
- PostgreSQL (production recommended)
- MySQL/MariaDB

**Configuration:**
```properties
# Repository type
cache.repository.type=sql

# Database connection (SQLite example)
spring.datasource.url=jdbc:sqlite:./data/ashredis.db
spring.datasource.driver-class-name=org.sqlite.JDBC

# PostgreSQL example
# spring.datasource.url=jdbc:postgresql://localhost:5432/ashredis
# spring.datasource.username=postgres
# spring.datasource.password=password
```

**Advantages:**
- ACID compliance
- SQL query capabilities
- Backup and restore support
- Wide tooling support
- Good for structured data

**Schema:**
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

---

### RocksDB Backend

**Features:**
- Embedded key-value store
- High performance
- Low latency
- Optimized for SSDs

**Configuration:**
```properties
# Repository type
cache.repository.type=rocksdb

# Base path for RocksDB data
cache.rocksdb.base.path=./data/rocksdb
```

**Advantages:**
- Very fast reads and writes
- Low memory footprint
- Efficient compression
- Good for high-throughput scenarios

**Use Cases:**
- High-performance caching
- Low-latency requirements
- Large datasets
- Edge deployments

---

## Network Protocol

### Redis Protocol Compatibility

AshRedis implements the Redis Serialization Protocol (RESP), making it compatible with standard Redis clients.

**Protocol Format:**

| Type | Prefix | Example |
|------|--------|---------|
| Simple String | `+` | `+OK` |
| Error | `-` | `-ERR unknown command` |
| Integer | `:` | `:42` |
| Bulk String | `$` | `$5\r\nhello` |
| Array | `*` | `*2\r\n$3\r\nfoo\r\n$3\r\nbar` |

**Connection:**
```bash
# Default port
telnet localhost 6379

# Using redis-cli
redis-cli -h localhost -p 6379
```

**Configuration:**
```properties
# Network server settings
network.server.enabled=true
network.server.port=6379
network.server.bind.address=0.0.0.0
```

---

### Enhanced Parser Features

**Quoted Strings:**
```bash
SET key "value with spaces"
SET key 'single quoted value'
```

**Escaped Characters:**
```bash
SET key "He said \"Hello\""
SET key "Line 1\nLine 2"
SET key "Tab\tseparated"
```

**JSON Payloads:**
```bash
SET user:1 "{\"name\": \"John\", \"age\": 30}"
```

**Special Characters:**
```bash
SET key "Path: C:\\Users\\John"
```

---

## Region Management

### What are Regions?

Regions provide logical partitioning of data within a single AshRedis instance. They enable:
- Data isolation
- Multi-tenancy
- Namespace management
- Independent eviction policies

### Using Regions

**Via Network Protocol:**
```bash
# Set with region
SET @users key1 value1

# Get from region
GET @users key1

# Without region (uses default)
SET key2 value2
```

**Via Java Client:**
```java
// Set default region
client.useRegion("users");

// Operations use default region
client.set("key1", "value1");

// Or specify region explicitly
client.setInRegion("users", "key1", "value1");
```

**Via Python Client:**
```python
# Set default region
client.use_region('users')

# Operations use default region
client.set('key1', 'value1')

# Or specify region explicitly
client.set('key1', 'value1', region='users')
```

**Via REST API:**
```bash
# Set in region
curl -X POST "http://localhost:8080/cache/set?region=users&key=key1&value=value1"

# Get from region
curl "http://localhost:8080/cache/get?region=users&key=key1"
```

### Default Region

**Configuration:**
```properties
cache.default.region=region0
```

When no region is specified, operations use the default region.

### Region Operations

**Get All Regions:**
```bash
GET /regions
```

**Get Region Statistics:**
```bash
GET /stats/{region}
```

**Delete Region:**
```bash
DELETE /regions/{region}
```

**Region Stats Include:**
- Total keys
- In-memory keys
- Disk keys
- Memory usage
- Hit/miss ratio

---

## Replication and High Availability

### Kafka-Based Replication

AshRedis uses Kafka for event-driven replication between instances.

**Architecture:**

```
Primary Instance              Secondary Instance(s)
      │                              │
      ├──► Write Operation           │
      │         │                    │
      │         ├──► Kafka Topic     │
      │         │    (replication)   │
      │         │                    │
      │         └─────────────────►  │
      │                              │
      └──► Heartbeat ────────────►  Kafka Topic ─────► Monitor
                                     (health)
```

**Configuration:**

**Primary Instance:**
```properties
# Enable clustering
cluster.enabled=true
cluster.instance.id=primary-1
cluster.mode=primary

# Kafka settings
kafka.replication.enabled=true
kafka.replication.topic=ashredis-replication
kafka.health.topic=ashredis-health
spring.kafka.bootstrap-servers=localhost:9092

# Failover settings
cluster.health.check.interval.ms=5000
cluster.failover.timeout.ms=15000
cluster.auto.promote.on.no.primary=true
```

**Secondary Instance:**
```properties
cluster.enabled=true
cluster.instance.id=secondary-1
cluster.mode=secondary

kafka.replication.enabled=true
kafka.replication.topic=ashredis-replication
kafka.health.topic=ashredis-health
spring.kafka.bootstrap-servers=localhost:9092
```

### Replicated Operations

The following operations are replicated:
- `SET` - Create/update entries
- `DELETE` - Delete entries
- `EXPIRE` - Set expiration
- `DELETE_REGION` - Delete entire region

### Automatic Failover

**How It Works:**

1. **Heartbeat Monitoring**
   - All instances send heartbeats every 5 seconds
   - Heartbeats include instance ID, role (primary/secondary), timestamp

2. **Failure Detection**
   - Secondary monitors primary heartbeats
   - If no heartbeat for 15 seconds (configurable), failover initiated

3. **Election Process**
   - Lexicographically smallest instance ID becomes primary
   - New primary sends immediate heartbeat
   - Begins accepting writes

4. **Automatic Recovery**
   - When failed primary recovers, it becomes secondary
   - No manual intervention required

**Configuration:**
```properties
# Health check interval
cluster.health.check.interval.ms=5000

# Failover timeout
cluster.failover.timeout.ms=15000

# Auto-promote on startup if no primary
cluster.auto.promote.on.no.primary=true

# Wait time on startup to detect primary
cluster.startup.wait.for.primary.ms=10000
```

### Monitoring Cluster Health

**Check Instance Role:**
```bash
curl http://localhost:8080/info
```

**View Replication Stats:**
```bash
curl http://localhost:8080/replication/status
```

**Logs to Monitor:**
```log
INFO - Replication service initialized - Instance ID: primary-1, Initial Mode: PRIMARY
INFO - Sent heartbeat: {"instanceId":"primary-1","isPrimary":true,"timestamp":1234567890}
WARN - Primary heartbeat timeout detected. Time since last: 16000ms
INFO - This instance is now PRIMARY: secondary-1
```

---

## Pub/Sub Messaging

### Channel-Based Pub/Sub

Traditional Redis-style pub/sub with channels.

**Subscribe to Channel:**

**Java:**
```java
pubSubService.subscribe("news", message -> {
    System.out.println("Received: " + message);
});
```

**Python:**
```python
def on_message(message):
    print(f"Received: {message}")

client.subscribe('news', on_message)
```

**Publish to Channel:**

**REST API:**
```bash
curl -X POST "http://localhost:8080/pubsub/publish?channel=news&message=Hello"
```

**Java:**
```java
long subscribers = pubSubService.publish("news", "Hello World");
```

---

### Region-Based Subscriptions

Subscribe to all changes in a region.

**Java:**
```java
pubSubService.subscribeToRegion("users", event -> {
    System.out.println("Region: " + event.getRegion());
    System.out.println("Key: " + event.getKey());
    System.out.println("Operation: " + event.getOperation());
});
```

**Events Generated:**
- `SET` - When key is created/updated
- `DEL` - When key is deleted
- `DELETE_REGION` - When region is deleted

---

### Key-Based Subscriptions

Subscribe to changes for a specific key.

**Java:**
```java
pubSubService.subscribeToKey("users", "user:123", event -> {
    System.out.println("Key updated: " + event.getKey());
});
```

---

### Subscriber Management

**Get Subscriber Counts:**
```java
Map<String, Integer> counts = pubSubService.getSubscriberCounts();
// {channels: 5, regions: 3, keys: 12}
```

**Unsubscribe:**
```java
pubSubService.unsubscribe("news", subscriber);
pubSubService.unsubscribeFromRegion("users", subscriber);
pubSubService.unsubscribeFromKey("users", "user:123", subscriber);
```

---

## Asynchronous Publishing

### PUBTODEST Command

Publish cache entries to external destinations asynchronously.

**Supported Destinations:**
- File system (`file://`)
- Kafka topics (`kafka://`)
- ActiveMQ queues (`activemq://`)
- ActiveMQ topics (`activemq://`)

### Usage

**File Destination:**
```bash
curl -X POST "http://localhost:8080/asyncpub/pubtodest" \
  -d "region=users" \
  -d "keyPattern=user:*" \
  -d "destination=file:///tmp/users.json"
```

**Kafka Destination:**
```bash
curl -X POST "http://localhost:8080/asyncpub/pubtodest" \
  -d "region=users" \
  -d "keyPattern=user:*" \
  -d "destination=kafka://default/topic/user-updates"
```

**ActiveMQ Queue:**
```bash
curl -X POST "http://localhost:8080/asyncpub/pubtodest" \
  -d "region=orders" \
  -d "keyPattern=order:*" \
  -d "destination=activemq://default/queue/orders"
```

### Key Pattern Matching

**Glob Pattern:**
```
keyPattern=user:*          # All keys starting with user:
keyPattern=*:active        # All keys ending with :active
keyPattern=user:?:profile  # Single character wildcard
```

**Regex Pattern:**
```
keyPattern=regex:^user:[0-9]+$    # Keys matching regex
```

**Comma-Separated List:**
```
keyPattern=key1,key2,key3  # Specific keys
```

**Single Key:**
```
keyPattern=user:123        # Exact key
```

### Publication Status

**Get Status:**
```bash
curl "http://localhost:8080/asyncpub/status/{uuid}"
```

**Response:**
```json
{
  "uuid": "abc-123",
  "region": "users",
  "keyPattern": "user:*",
  "destination": "file:///tmp/users.json",
  "status": "COMPLETED",
  "totalKeys": 1000,
  "publishedKeys": 1000,
  "errors": 0,
  "startTime": 1234567890,
  "endTime": 1234567900,
  "durationMs": 10000
}
```

**Status Values:**
- `INITIATED` - Publication started
- `IN_PROGRESS` - Currently publishing
- `COMPLETED` - Successfully completed
- `FAILED` - Failed with error

**Get All Publications:**
```bash
curl "http://localhost:8080/asyncpub/publications"
```

### Output Format

**JSON Format:**
```json
--begin uuid: abc-123--
{"region":"users","key":"user:1","value":"John","timestamp":1234567890}
{"region":"users","key":"user:2","value":"Jane","timestamp":1234567891}
--end uuid: abc-123--
```

### Configuration

**ActiveMQ:**
```properties
activemq.enabled=true
activemq.broker.url=tcp://localhost:61616
```

**Kafka:**
```properties
kafka.async.enabled=true
spring.kafka.bootstrap-servers=localhost:9092
```

---

## Transaction Support

### MULTI/EXEC/DISCARD

Standard Redis transaction support.

**Transaction Workflow:**

```
MULTI                    # Start transaction
QUEUED                   # All commands queued
QUEUED
QUEUED
EXEC                     # Execute all commands
[result1, result2, ...]  # Results array
```

### Using Transactions

**Via Network Protocol:**
```bash
MULTI
SET key1 value1
SET key2 value2
INCR counter
EXEC
```

**Via TransactionService (Java):**
```java
// Start transaction
transactionService.multi(sessionId);

// Queue commands
transactionService.queueCommand(sessionId, 
    () -> cacheService.set(region, "key1", "value1", null),
    "SET key1");
    
transactionService.queueCommand(sessionId,
    () -> cacheService.incr(region, "counter"),
    "INCR counter");

// Execute
List<String> results = transactionService.exec(sessionId);
```

### DISCARD

Discard all queued commands without executing.

```bash
MULTI
SET key1 value1
SET key2 value2
DISCARD    # Abort transaction
```

### Transaction Features

**Atomic Execution:**
- All commands execute together
- No interleaving with other commands
- Either all succeed or all fail

**Error Handling:**
- Syntax errors detected during QUEUE phase
- Runtime errors reported in results
- Partial execution not possible

**Session Management:**
- Transactions tied to session ID
- Automatic cleanup on disconnect
- Multiple concurrent transactions supported

### Limitations

- No optimistic locking (WATCH/UNWATCH)
- No conditional execution within transaction
- Commands executed sequentially, not in parallel

---

## Security and Authentication

### User-Based Authentication

**User Model:**
```json
{
  "userid": "admin",
  "name": "Administrator",
  "password": "admin",
  "roles": ["ADMIN", "USER"]
}
```

### User Configuration

**users.json:**
```json
{
  "users": [
    {
      "userid": "admin",
      "name": "Administrator",
      "password": "admin",
      "roles": ["ADMIN", "USER"]
    },
    {
      "userid": "developer",
      "name": "Developer User",
      "password": "dev123",
      "roles": ["USER"]
    }
  ]
}
```

**Location:** `src/main/resources/users.json`

### Authentication

**REST API Authentication:**
```bash
# Basic Auth
curl -u admin:admin "http://localhost:8080/cache/get?key=mykey"

# With explicit headers
curl -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  "http://localhost:8080/cache/get?key=mykey"
```

### Role-Based Access Control

**Roles:**
- `ADMIN` - Full access to all operations
- `USER` - Standard cache operations
- Custom roles can be defined

**Role Checks:**
```java
// Check if user has role
boolean isAdmin = userService.hasRole(user, "ADMIN");

// Check if user is admin
boolean isAdmin = userService.isAdmin(user);
```

### Security Best Practices

1. **Change Default Credentials**
   - Never use default admin/admin in production
   - Use strong passwords

2. **Network Security**
   - Use firewall rules to restrict access
   - Consider VPN or SSH tunneling
   - Use SSL/TLS for REST API

3. **Authentication**
   - Implement proper authentication layer
   - Use JWT tokens for REST API
   - Rotate credentials regularly

4. **Authorization**
   - Implement fine-grained permissions
   - Audit access logs
   - Monitor suspicious activity

---

## REST API

### Base URL

```
http://localhost:8080
```

### Cache Operations

**SET - Store Value:**
```bash
POST /cache/set
Parameters:
  - region (optional): Region name
  - key (required): Key name
  - value (required): Value to store
  - ttl (optional): Time-to-live in seconds

Example:
curl -X POST "http://localhost:8080/cache/set?key=mykey&value=myvalue&ttl=60"
```

**GET - Retrieve Value:**
```bash
GET /cache/get
Parameters:
  - region (optional): Region name
  - key (required): Key name

Example:
curl "http://localhost:8080/cache/get?key=mykey"
```

**DELETE - Remove Keys:**
```bash
DELETE /cache/del
Parameters:
  - region (optional): Region name
  - keys (required): Comma-separated keys

Example:
curl -X DELETE "http://localhost:8080/cache/del?keys=key1,key2,key3"
```

**EXISTS - Check Existence:**
```bash
GET /cache/exists
Parameters:
  - region (optional): Region name
  - keys (required): Comma-separated keys

Example:
curl "http://localhost:8080/cache/exists?keys=key1,key2"

Response: {"count": 2}
```

**KEYS - Pattern Matching:**
```bash
GET /cache/keys
Parameters:
  - region (optional): Region name
  - pattern (optional): Glob pattern (default: *)

Example:
curl "http://localhost:8080/cache/keys?pattern=user:*"

Response: ["user:1", "user:2", "user:3"]
```

---

### Expiration Operations

**EXPIRE - Set TTL:**
```bash
POST /cache/expire
Parameters:
  - region (optional): Region name
  - key (required): Key name
  - seconds (required): TTL in seconds

Example:
curl -X POST "http://localhost:8080/cache/expire?key=mykey&seconds=300"
```

**TTL - Get Remaining Time:**
```bash
GET /cache/ttl
Parameters:
  - region (optional): Region name
  - key (required): Key name

Example:
curl "http://localhost:8080/cache/ttl?key=mykey"

Response: {"ttl": 295}
```

**PERSIST - Remove Expiration:**
```bash
POST /cache/persist
Parameters:
  - region (optional): Region name
  - key (required): Key name

Example:
curl -X POST "http://localhost:8080/cache/persist?key=mykey"
```

---

### Region Operations

**Get All Regions:**
```bash
GET /regions

Example:
curl "http://localhost:8080/regions"

Response: ["region0", "users", "sessions"]
```

**Get Region Stats:**
```bash
GET /stats/{region}

Example:
curl "http://localhost:8080/stats/users"

Response:
{
  "totalKeys": 1000,
  "inMemoryKeys": 500,
  "diskKeys": 500,
  "memoryUsage": "50MB",
  "hitRate": 0.85
}
```

**Delete Region:**
```bash
DELETE /regions/{region}

Example:
curl -X DELETE "http://localhost:8080/regions/users"
```

---

### Pub/Sub Operations

**Publish Message:**
```bash
POST /pubsub/publish
Parameters:
  - channel (required): Channel name
  - message (required): Message content

Example:
curl -X POST "http://localhost:8080/pubsub/publish?channel=news&message=Hello"

Response: {"subscribers": 5}
```

**Get Subscriber Counts:**
```bash
GET /pubsub/subscribers

Example:
curl "http://localhost:8080/pubsub/subscribers"

Response:
{
  "channels": 10,
  "regions": 5,
  "keys": 25
}
```

---

### Async Publishing

**Publish to Destination:**
```bash
POST /asyncpub/pubtodest
Parameters:
  - region (required): Region name
  - keyPattern (required): Key pattern
  - destination (required): Destination URL

Example:
curl -X POST "http://localhost:8080/asyncpub/pubtodest" \
  -d "region=users" \
  -d "keyPattern=user:*" \
  -d "destination=file:///tmp/users.json"

Response: {"uuid": "abc-123-def-456"}
```

**Get Publication Status:**
```bash
GET /asyncpub/status/{uuid}

Example:
curl "http://localhost:8080/asyncpub/status/abc-123"
```

**Get All Publications:**
```bash
GET /asyncpub/publications

Example:
curl "http://localhost:8080/asyncpub/publications"
```

---

### Command Operations

**INCR - Increment:**
```bash
POST /command/incr
Parameters:
  - region (optional): Region name
  - key (required): Key name

Example:
curl -X POST "http://localhost:8080/command/incr?key=counter"

Response: {"value": 42}
```

**DECR - Decrement:**
```bash
POST /command/decr
Parameters:
  - region (optional): Region name
  - key (required): Key name

Example:
curl -X POST "http://localhost:8080/command/decr?key=counter"

Response: {"value": 41}
```

**APPEND - Append String:**
```bash
POST /command/append
Parameters:
  - region (optional): Region name
  - key (required): Key name
  - value (required): Value to append

Example:
curl -X POST "http://localhost:8080/command/append?key=log&value=,newentry"

Response: {"length": 150}
```

---

### System Operations

**INFO - System Information:**
```bash
GET /info

Example:
curl "http://localhost:8080/info"

Response:
{
  "version": "3.0",
  "regions": ["region0", "users"],
  "uptime_seconds": 86400,
  "memory": {
    "used": "250MB",
    "max": "1GB",
    "percent": 25
  }
}
```

**PING - Health Check:**
```bash
GET /ping

Example:
curl "http://localhost:8080/ping"

Response: "PONG"
```

---

## Client Libraries

### Java Client

**Installation:**
```xml
<!-- Add to your project -->
<dependency>
    <groupId>com.ash.projects</groupId>
    <artifactId>ashredis-client</artifactId>
    <version>3.0</version>
</dependency>
```

**Usage:**
```java
import com.ash.projects.redisclone.client.AshRedisClient;

// Create client
AshRedisClient client = new AshRedisClient("localhost", 6379);

// Connect
client.connect();

// Set default region
client.useRegion("users");

// Basic operations
client.set("key1", "value1");
String value = client.get("key1");

// With expiration
client.setInRegion("users", "session:123", "data", 3600L);

// Increment
long counter = client.incr("counter");

// Pattern matching
Set<String> keys = client.keys("user:*");

// Delete
long deleted = client.del("key1", "key2");

// Close
client.close();
```

**Try-with-resources:**
```java
try (AshRedisClient client = new AshRedisClient("localhost", 6379)) {
    client.connect();
    client.set("mykey", "myvalue");
    System.out.println(client.get("mykey"));
} // Automatically closed
```

---

### Python Client

**Installation:**
```bash
pip install ashredis-client
```

**Usage:**
```python
from ashredis_client import AshRedisClient

# Create client
client = AshRedisClient('localhost', 6379)

# Connect
client.connect()

# Set default region
client.use_region('users')

# Basic operations
client.set('key1', 'value1')
value = client.get('key1')

# With expiration
client.set('session:123', 'data', ttl_seconds=3600)

# Increment
counter = client.incr('counter')

# Pattern matching
keys = client.keys('user:*')

# Delete
deleted = client.delete('key1', 'key2')

# Close
client.close()
```

**Context Manager:**
```python
with AshRedisClient('localhost', 6379) as client:
    client.set('mykey', 'myvalue')
    print(client.get('mykey'))
# Automatically closed
```

**Async Operations:**
```python
# Subscribe to channel
def on_message(message):
    print(f"Received: {message}")

client.subscribe('news', on_message)

# Keep running to receive messages
# Messages processed in background thread
```

---

## Configuration Reference

### Complete Configuration Template

```properties
# ============================================
# BASIC SETTINGS
# ============================================

# Application name
spring.application.name=ashredis

# Server port (REST API)
server.port=8080

# Default region
cache.default.region=region0

# ============================================
# CACHE CONFIGURATION
# ============================================

# Maximum objects in memory
cache.max.memory.objects=100000

# Repository type: sql or rocksdb
cache.repository.type=sql

# RocksDB path (if using rocksdb)
cache.rocksdb.base.path=./data/rocksdb

# Cleanup interval for expired entries (seconds)
cache.cleanup.interval.seconds=60

# ============================================
# HEAP MONITORING
# ============================================

# Enable heap-based auto-eviction
cache.heap.monitor.enabled=true

# Heap threshold percentage
cache.heap.threshold.percent=80

# Eviction batch size
cache.heap.eviction.batch.size=1000

# Monitoring interval (ms)
cache.heap.monitor.interval.ms=30000

# ============================================
# DATABASE (SQL)
# ============================================

# SQLite (default)
spring.datasource.url=jdbc:sqlite:./data/ashredis.db
spring.datasource.driver-class-name=org.sqlite.JDBC

# PostgreSQL (production)
# spring.datasource.url=jdbc:postgresql://localhost:5432/ashredis
# spring.datasource.username=postgres
# spring.datasource.password=password
# spring.datasource.driver-class-name=org.postgresql.Driver

# MySQL (alternative)
# spring.datasource.url=jdbc:mysql://localhost:3306/ashredis
# spring.datasource.username=root
# spring.datasource.password=password
# spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Connection pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5

# ============================================
# NETWORK SERVER
# ============================================

# Enable network server
network.server.enabled=true

# Network port (Redis protocol)
network.server.port=6379

# Bind address
network.server.bind.address=0.0.0.0

# ============================================
# REPLICATION (KAFKA)
# ============================================

# Enable Kafka replication
kafka.replication.enabled=false

# Kafka settings
spring.kafka.bootstrap-servers=localhost:9092
kafka.replication.topic=ashredis-replication
kafka.health.topic=ashredis-health

# Consumer group
spring.kafka.consumer.group-id=ashredis-cluster

# ============================================
# CLUSTERING
# ============================================

# Enable clustering
cluster.enabled=false

# Instance ID (must be unique)
cluster.instance.id=instance-1

# Initial mode: primary or secondary
cluster.mode=primary

# Health check interval (ms)
cluster.health.check.interval.ms=5000

# Failover timeout (ms)
cluster.failover.timeout.ms=15000

# Auto-promote on no primary
cluster.auto.promote.on.no.primary=true

# Startup wait for primary (ms)
cluster.startup.wait.for.primary.ms=10000

# ============================================
# ASYNC PUBLISHING
# ============================================

# ActiveMQ
activemq.enabled=false
activemq.broker.url=tcp://localhost:61616

# Kafka async
kafka.async.enabled=false

# ============================================
# LOGGING
# ============================================

# Log level
logging.level.root=INFO
logging.level.com.ash.projects=DEBUG

# Log file
logging.file.name=logs/ashredis.log
logging.file.max-size=10MB
logging.file.max-history=30

# ============================================
# MONITORING
# ============================================

# Actuator endpoints
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always

# ============================================
# SECURITY
# ============================================

# User configuration file
users.config.file=classpath:users.json
```

---

## Deployment Guide

### Development Deployment

**Requirements:**
- JDK 17 or higher
- Maven 3.6+

**Steps:**

1. **Clone Repository**
   ```bash
   git clone https://github.com/ashsinha/ashredis.git
   cd ashredis
   ```

2. **Configure**
   ```bash
   cp src/main/resources/application.properties.example \
      src/main/resources/application.properties
   # Edit application.properties
   ```

3. **Build**
   ```bash
   mvn clean package
   ```

4. **Run**
   ```bash
   java -jar target/ashredis-3.0.jar
   ```

5. **Verify**
   ```bash
   curl http://localhost:8080/ping
   redis-cli -h localhost -p 6379 PING
   ```

---

### Production Deployment

**Requirements:**
- JDK 17+ (production JVM)
- PostgreSQL or MySQL (recommended over SQLite)
- Kafka (if using replication)
- Load balancer (for multiple instances)

**Configuration:**

1. **application-prod.properties**
   ```properties
   # Use PostgreSQL
   spring.datasource.url=jdbc:postgresql://db-host:5432/ashredis
   spring.datasource.username=${DB_USER}
   spring.datasource.password=${DB_PASSWORD}
   
   # Production heap settings
   cache.heap.threshold.percent=85
   cache.max.memory.objects=500000
   
   # Enable replication
   kafka.replication.enabled=true
   spring.kafka.bootstrap-servers=kafka1:9092,kafka2:9092,kafka3:9092
   
   # Clustering
   cluster.enabled=true
   cluster.instance.id=${INSTANCE_ID}
   cluster.mode=${CLUSTER_MODE}
   
   # Security
   # (Add SSL, authentication, etc.)
   ```

2. **JVM Settings**
   ```bash
   java -Xms4g -Xmx8g \
        -XX:+UseG1GC \
        -XX:MaxGCPauseMillis=200 \
        -XX:+HeapDumpOnOutOfMemoryError \
        -XX:HeapDumpPath=/var/log/ashredis/heap.hprof \
        -Dspring.profiles.active=prod \
        -jar ashredis-3.0.jar
   ```

3. **Systemd Service**
   ```ini
   [Unit]
   Description=AshRedis Service
   After=network.target
   
   [Service]
   Type=simple
   User=ashredis
   WorkingDirectory=/opt/ashredis
   ExecStart=/usr/bin/java -Xms4g -Xmx8g \
             -XX:+UseG1GC \
             -jar /opt/ashredis/ashredis-3.0.jar
   Restart=on-failure
   RestartSec=10
   
   [Install]
   WantedBy=multi-user.target
   ```

4. **Enable and Start**
   ```bash
   sudo systemctl enable ashredis
   sudo systemctl start ashredis
   sudo systemctl status ashredis
   ```

---

### Docker Deployment

**Dockerfile:**
```dockerfile
FROM openjdk:17-slim

LABEL maintainer="ajsinha@gmail.com"
LABEL version="3.0"

# Create app directory
WORKDIR /app

# Copy jar
COPY target/ashredis-3.0.jar app.jar

# Create data directory
RUN mkdir -p /app/data

# Expose ports
EXPOSE 6379 8080

# JVM options
ENV JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC"

# Run
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**Build Image:**
```bash
docker build -t ashredis:3.0 .
```

**Run Container:**
```bash
docker run -d \
  --name ashredis \
  -p 6379:6379 \
  -p 8080:8080 \
  -v ashredis-data:/app/data \
  -e SPRING_PROFILES_ACTIVE=prod \
  ashredis:3.0
```

**Docker Compose:**
```yaml
version: '3.8'

services:
  ashredis:
    image: ashredis:3.0
    ports:
      - "6379:6379"
      - "8080:8080"
    volumes:
      - ashredis-data:/app/data
      - ./application.properties:/app/config/application.properties
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JAVA_OPTS=-Xms2g -Xmx4g
    restart: unless-stopped
    
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: ashredis
      POSTGRES_USER: ashredis
      POSTGRES_PASSWORD: password
    volumes:
      - postgres-data:/var/lib/postgresql/data
    
  kafka:
    image: confluentinc/cp-kafka:latest
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    depends_on:
      - zookeeper
      
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

volumes:
  ashredis-data:
  postgres-data:
```

---

### Kubernetes Deployment

**deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ashredis
  labels:
    app: ashredis
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ashredis
  template:
    metadata:
      labels:
        app: ashredis
    spec:
      containers:
      - name: ashredis
        image: ashredis:3.0
        ports:
        - containerPort: 6379
          name: redis
        - containerPort: 8080
          name: http
        env:
        - name: CLUSTER_ENABLED
          value: "true"
        - name: CLUSTER_INSTANCE_ID
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: KAFKA_BOOTSTRAP_SERVERS
          value: "kafka:9092"
        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /ping
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ping
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: ashredis
spec:
  selector:
    app: ashredis
  ports:
  - name: redis
    port: 6379
    targetPort: 6379
  - name: http
    port: 8080
    targetPort: 8080
  type: LoadBalancer
```

**Apply:**
```bash
kubectl apply -f deployment.yaml
```

---

## Monitoring and Operations

### Logging

**Log Levels:**
- `TRACE` - Very detailed debugging
- `DEBUG` - Debugging information
- `INFO` - Informational messages
- `WARN` - Warning messages
- `ERROR` - Error messages

**Configuration:**
```properties
# Root level
logging.level.root=INFO

# Package-specific
logging.level.com.ash.projects.redisclone=DEBUG
logging.level.com.ash.projects.redisclone.service.CacheService=TRACE

# File logging
logging.file.name=logs/ashredis.log
logging.file.max-size=10MB
logging.file.max-history=30
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

**Important Log Messages:**

**Startup:**
```log
INFO - Starting AshRedis...
INFO - CacheRepository initialized with SQL implementation
INFO - Cache Service initialized successfully
INFO - Network server started on 0.0.0.0:6379
```

**Heap Monitoring:**
```log
DEBUG - Heap usage: 1638/2048 MB (80.0%)
WARN  - Heap threshold exceeded: 85.2% >= 80%
INFO  - Heap-based eviction complete: evicted 1000 entries
```

**Replication:**
```log
INFO - Replication service initialized - Instance ID: primary-1
INFO - Sent heartbeat: {"instanceId":"primary-1","isPrimary":true}
WARN - Primary heartbeat timeout detected
INFO - This instance is now PRIMARY: secondary-1
```

**Errors:**
```log
ERROR - Error processing command: SET key value
ERROR - Database connection lost
ERROR - Failed to replicate operation
```

---

### Metrics

**Available Metrics:**
- Total requests
- Cache hits/misses
- Memory usage
- Heap usage
- Replication lag
- Network throughput
- Operation latencies

**Actuator Endpoints:**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/cache.requests.total
```

**Prometheus Integration:**
```properties
# Enable Prometheus endpoint
management.endpoints.web.exposure.include=prometheus
management.metrics.export.prometheus.enabled=true
```

**Access:**
```bash
curl http://localhost:8080/actuator/prometheus
```

---

### Health Checks

**HTTP Health Check:**
```bash
curl http://localhost:8080/ping
# Response: PONG
```

**Actuator Health:**
```bash
curl http://localhost:8080/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "SQLite",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000,
        "free": 250000000,
        "threshold": 10485760
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

---

### Performance Monitoring

**Key Metrics to Monitor:**

1. **Cache Hit Rate**
   - Target: > 80%
   - Low hit rate indicates undersized cache

2. **Heap Usage**
   - Target: 60-80% average
   - > 85% sustained triggers eviction

3. **Eviction Frequency**
   - Target: < 10/hour
   - High frequency indicates memory pressure

4. **Response Time**
   - In-memory GET: < 1ms
   - Disk GET: < 10ms
   - SET: < 2ms

5. **Replication Lag**
   - Target: < 100ms
   - High lag indicates network or Kafka issues

**Monitoring Commands:**

**Watch Heap:**
```bash
tail -f logs/ashredis.log | grep -E "Heap|evict"
```

**Count Requests:**
```bash
grep "Received command" logs/ashredis.log | wc -l
```

**Calculate Hit Rate:**
```bash
grep "Cache hit" logs/ashredis.log | wc -l
grep "Cache miss" logs/ashredis.log | wc -l
```

**Monitor Replication:**
```bash
tail -f logs/ashredis.log | grep -E "Replicated|heartbeat"
```

---

## Performance Tuning

### Memory Tuning

**Heap Size:**
```bash
# Small (1-2GB)
java -Xms1g -Xmx2g -jar ashredis.jar

# Medium (4-8GB)
java -Xms4g -Xmx8g -jar ashredis.jar

# Large (16GB+)
java -Xms16g -Xmx32g -jar ashredis.jar
```

**Cache Size:**
```properties
# Conservative (10% of heap)
cache.max.memory.objects=100000

# Moderate (30% of heap)
cache.max.memory.objects=500000

# Aggressive (50% of heap)
cache.max.memory.objects=1000000
```

**Heap Monitoring:**
```properties
# Development
cache.heap.threshold.percent=75
cache.heap.eviction.batch.size=500
cache.heap.monitor.interval.ms=15000

# Production
cache.heap.threshold.percent=85
cache.heap.eviction.batch.size=2000
cache.heap.monitor.interval.ms=60000
```

---

### GC Tuning

**G1GC (Recommended):**
```bash
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:G1ReservePercent=10 \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -jar ashredis.jar
```

**ParallelGC (High Throughput):**
```bash
java -XX:+UseParallelGC \
     -XX:ParallelGCThreads=8 \
     -jar ashredis.jar
```

**ZGC (Low Latency, Java 15+):**
```bash
java -XX:+UseZGC \
     -XX:ZAllocationSpikeTolerance=5 \
     -jar ashredis.jar
```

---

### Database Tuning

**SQLite:**
```properties
# Use WAL mode for better concurrency
spring.datasource.url=jdbc:sqlite:./data/ashredis.db?journal_mode=WAL

# Connection pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

**PostgreSQL:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ashredis?currentSchema=public

# Connection pool (larger for high concurrency)
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

**RocksDB:**
```properties
cache.repository.type=rocksdb
cache.rocksdb.base.path=/fast-ssd/rocksdb
```

---

### Network Tuning

**Apache MINA:**
```properties
# Buffer sizes
mina.read.buffer.size=4096
mina.write.buffer.size=4096

# Thread pool
mina.processor.threads=8
```

**REST API:**
```properties
# Thread pool
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10

# Connection timeout
server.tomcat.connection-timeout=20000
```

---

### Replication Tuning

**Kafka Settings:**
```properties
# Producer settings
spring.kafka.producer.acks=1
spring.kafka.producer.batch-size=16384
spring.kafka.producer.buffer-memory=33554432
spring.kafka.producer.compression-type=snappy

# Consumer settings
spring.kafka.consumer.fetch-min-size=1024
spring.kafka.consumer.fetch-max-wait=500
spring.kafka.consumer.max-poll-records=500

# Health check
cluster.health.check.interval.ms=5000
cluster.failover.timeout.ms=15000
```

---

## Advanced Features

### Custom Eviction Policies

Implement custom eviction strategies by extending CacheService.

**Example: Size-Based Eviction**
```java
@Override
protected boolean shouldEvict(CacheEntry entry) {
    // Evict large entries first
    return entry.getValue().toString().length() > 10000;
}
```

---

### Batch Operations

Process multiple keys efficiently.

**Java Example:**
```java
// Batch SET
List<String> keys = Arrays.asList("key1", "key2", "key3");
List<String> values = Arrays.asList("val1", "val2", "val3");

for (int i = 0; i < keys.size(); i++) {
    cacheService.set(region, keys.get(i), values.get(i), null);
}

// Batch GET
List<String> results = new ArrayList<>();
for (String key : keys) {
    results.add(cacheService.get(region, key));
}
```

---

### Data Migration

**SQL to RocksDB:**
```java
// Export from SQL
CacheRepositorySQL sqlRepo = cacheRepository.getSqlRepository();
List<CacheEntry> entries = sqlRepo.loadAllEntries();

// Import to RocksDB
// 1. Stop application
// 2. Change config: cache.repository.type=rocksdb
// 3. Start application
// 4. Load entries programmatically

for (CacheEntry entry : entries) {
    rocksDbRepo.saveEntry(entry);
}
```

---

### Custom Serialization

Implement custom serialization for complex objects.

**Example:**
```java
public class CustomSerializer {
    public static byte[] serialize(Object obj) {
        // Custom serialization logic
    }
    
    public static Object deserialize(byte[] bytes) {
        // Custom deserialization logic
    }
}
```

---

## Troubleshooting

### Common Issues

**Issue 1: OutOfMemoryError**

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions:**
1. Increase heap size: `-Xmx8g`
2. Lower heap threshold: `cache.heap.threshold.percent=70`
3. Increase eviction batch: `cache.heap.eviction.batch.size=2000`
4. Reduce memory objects: `cache.max.memory.objects=50000`

---

**Issue 2: High Database I/O**

**Symptoms:**
- Slow response times
- High disk usage
- Database connection pool exhausted

**Solutions:**
1. Increase connection pool: `spring.datasource.hikari.maximum-pool-size=50`
2. Use RocksDB: `cache.repository.type=rocksdb`
3. Increase heap threshold: `cache.heap.threshold.percent=85`
4. Add database indexes
5. Use faster storage (SSD)

---

**Issue 3: Replication Lag**

**Symptoms:**
- Data inconsistency between instances
- Delayed updates on secondaries
- High Kafka consumer lag

**Solutions:**
1. Increase Kafka resources
2. Optimize network between instances
3. Increase consumer threads
4. Check Kafka topic configuration
5. Monitor Kafka cluster health

---

**Issue 4: Uneven Region Distribution**

**Symptoms:**
- Some regions have all memory
- Other regions starved

**Solution:**
This is already fixed with fair region distribution. Ensure you're using version 3.0+.

---

**Issue 5: Connection Timeout**

**Symptoms:**
```
Connection refused
Connection timeout
```

**Solutions:**
1. Check server is running: `curl http://localhost:8080/ping`
2. Check port binding: `netstat -an | grep 6379`
3. Check firewall rules
4. Verify network server enabled: `network.server.enabled=true`

---

### Debug Mode

**Enable Debug Logging:**
```properties
logging.level.com.ash.projects.redisclone=DEBUG
logging.level.com.ash.projects.redisclone.service.CacheService=TRACE
```

**View Detailed Logs:**
```bash
tail -f logs/ashredis.log
```

---

### Performance Profiling

**JVM Tools:**

**JConsole:**
```bash
jconsole
# Connect to AshRedis process
# Monitor memory, threads, CPU
```

**VisualVM:**
```bash
jvisualvm
# Attach to process
# Profile CPU and memory
```

**Heap Dump:**
```bash
jmap -dump:format=b,file=heap.hprof <pid>

# Analyze with Eclipse MAT or VisualVM
```

**Thread Dump:**
```bash
jstack <pid> > threads.txt
```

---

### Support and Community

**Getting Help:**
- Email: ajsinha@gmail.com
- GitHub Issues: (if available)
- Documentation: This document

**Reporting Bugs:**
Include:
- Version number
- Configuration file
- Log excerpts
- Steps to reproduce
- Expected vs actual behavior

---

## Appendix

### Command Reference

| Command | Syntax | Description |
|---------|--------|-------------|
| PING | `PING` | Test connection |
| SET | `SET [@region] key value [EX seconds]` | Store value |
| GET | `GET [@region] key` | Retrieve value |
| DEL | `DEL [@region] key [key ...]` | Delete keys |
| EXISTS | `EXISTS [@region] key [key ...]` | Check existence |
| EXPIRE | `EXPIRE [@region] key seconds` | Set expiration |
| TTL | `TTL [@region] key` | Get TTL |
| PERSIST | `PERSIST [@region] key` | Remove expiration |
| KEYS | `KEYS [@region] pattern` | Pattern matching |
| INCR | `INCR [@region] key` | Increment |
| DECR | `DECR [@region] key` | Decrement |
| APPEND | `APPEND [@region] key value` | Append string |
| INFO | `INFO` | System info |

---

### Configuration Properties Index

| Property | Default | Description |
|----------|---------|-------------|
| `cache.max.memory.objects` | 100000 | Max in-memory objects |
| `cache.default.region` | region0 | Default region name |
| `cache.repository.type` | sql | Repository type |
| `cache.heap.monitor.enabled` | true | Enable heap monitoring |
| `cache.heap.threshold.percent` | 80 | Heap threshold |
| `cache.heap.eviction.batch.size` | 1000 | Eviction batch size |
| `cache.heap.monitor.interval.ms` | 30000 | Monitor interval |
| `network.server.enabled` | true | Enable network server |
| `network.server.port` | 6379 | Network port |
| `kafka.replication.enabled` | false | Enable replication |
| `cluster.enabled` | false | Enable clustering |
| `cluster.mode` | primary | Instance mode |

---

### Error Codes

| Code | Message | Cause | Solution |
|------|---------|-------|----------|
| ERR-001 | Connection refused | Server not running | Start server |
| ERR-002 | Invalid command | Unknown command | Check syntax |
| ERR-003 | Wrong number of arguments | Incorrect args | Check documentation |
| ERR-004 | Key not found | Key doesn't exist | Verify key name |
| ERR-005 | Type mismatch | Wrong data type | Check key type |
| ERR-006 | Out of memory | Heap exhausted | Increase heap |
| ERR-007 | Database error | DB connection failed | Check database |
| ERR-008 | Replication error | Kafka unavailable | Check Kafka |

---

### Version History

**Version 3.0 (Current)**
- Fair region distribution
- Heap-based auto-eviction
- Shutdown persistence
- Enhanced command parser
- Production-ready features

**Version 2.0**
- Replication support
- Pub/Sub messaging
- Transaction support
- Multiple storage backends

**Version 1.0**
- Basic Redis compatibility
- Region support
- REST API
- Client libraries

---

### License and Copyright

**Copyright:** All Rights Reserved 2025-2030, Ashutosh Sinha  
**Author:** ajsinha@gmail.com  
**Project:** Abhikarta LLM Platform - AshRedis Clone

This software is proprietary and confidential. Unauthorized copying, modification, distribution, or use of this software, via any medium, is strictly prohibited without prior written consent from the author.

For licensing inquiries, contact: ajsinha@gmail.com

---

### Acknowledgments

Built with:
- Spring Boot
- Apache MINA
- RocksDB
- Apache Kafka
- PostgreSQL/MySQL/SQLite

Inspired by Redis and modern distributed caching systems.

---

**End of Documentation**

For questions, support, or licensing inquiries:  
**Email:** ajsinha@gmail.com

**Version:** 3.0  
**Last Updated:** January 2025  
**Copyright:** All Rights Reserved 2025-2030, Ashutosh Sinha
