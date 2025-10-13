# AshRedis Clone - Clustering Behavior

## Default Behavior

By default, AshRedis runs in **standalone mode** as a **PRIMARY** instance with clustering disabled.

## Configuration Options

### Single Instance (Default)
```properties
cluster.enabled=false
cluster.mode=primary
```

**Behavior:**
- Runs as PRIMARY
- No replication
- All operations allowed
- Perfect for development and testing

### Primary-Secondary Cluster
```properties
# Primary Instance
cluster.enabled=true
cluster.mode=primary
kafka.replication.enabled=true

# Secondary Instance
cluster.enabled=true
cluster.mode=secondary
kafka.replication.enabled=true
```

## Auto-Promotion Feature

When starting as **SECONDARY** with auto-promotion enabled (default):

1. **Waits 10 seconds** for primary heartbeat detection
2. **If no primary detected**, automatically promotes itself to PRIMARY
3. **If primary detected**, remains as SECONDARY

### Configuration
```properties
# Enable auto-promotion (default: true)
cluster.auto.promote.on.no.primary=true

# How long to wait for primary detection (default: 10000ms)
cluster.startup.wait.for.primary.ms=10000
```

## Running Scenarios

### Scenario 1: Single Instance
```bash
# Start without any clustering config
java -jar redisclone-1.0.0.jar

# Result: Runs as PRIMARY (clustering disabled)
```

### Scenario 2: Start Secondary First (Auto-Promotion)
```bash
# Start secondary with Kafka enabled
java -jar redisclone-1.0.0.jar \
  --cluster.enabled=true \
  --cluster.mode=secondary

# Result: 
# - Waits 10 seconds
# - No primary detected
# - Auto-promotes to PRIMARY
```

### Scenario 3: Start Primary Then Secondary
```bash
# Terminal 1 - Start Primary
java -jar redisclone-1.0.0.jar \
  --cluster.enabled=true \
  --cluster.mode=primary \
  --server.port=8080

# Terminal 2 - Start Secondary
java -jar redisclone-1.0.0.jar \
  --cluster.enabled=true \
  --cluster.mode=secondary \
  --server.port=8081

# Result:
# - Primary: Runs as PRIMARY
# - Secondary: Detects primary, remains SECONDARY
```

### Scenario 4: Disable Auto-Promotion
```bash
# Start secondary without auto-promotion
java -jar redisclone-1.0.0.jar \
  --cluster.enabled=true \
  --cluster.mode=secondary \
  --cluster.auto.promote.on.no.primary=false

# Result: Always remains SECONDARY even if no primary
```

## Instance Roles

### PRIMARY Instance
- ✅ Accepts all write operations (SET, DEL, etc.)
- ✅ Accepts read operations
- ✅ Replicates changes to secondary via Kafka
- ✅ Sends heartbeats
- ✅ Web UI shows "PRIMARY" badge (green)

### SECONDARY Instance
- ❌ Rejects write operations
- ✅ Accepts read operations
- ✅ Receives replicated changes
- ✅ Can promote to primary on failover
- ⚠️ Web UI shows "SECONDARY" badge (yellow)

## Failover Behavior

### Automatic Failover
When PRIMARY fails:
1. **Secondary detects** missing heartbeat (15s timeout)
2. **Election occurs** (instance with smallest ID wins)
3. **Winner promotes** to PRIMARY
4. **New primary** starts accepting writes

### Manual Failover
```bash
# Promote secondary to primary
curl -X POST http://localhost:8081/admin/promote

# Or restart as primary
java -jar redisclone-1.0.0.jar \
  --cluster.mode=primary \
  --server.port=8081
```

## Monitoring Instance Status

### Check via Web UI
- Login to http://localhost:8080
- Dashboard shows instance status badge
- Green = PRIMARY, Yellow = SECONDARY

### Check via Logs
```bash
# Look for these log messages:
# Clustering disabled - Running as PRIMARY
# Replication service initialized - Instance ID: xxx, Initial Mode: PRIMARY
# No primary detected after 10000 ms. Auto-promoting to PRIMARY.
# This instance is now PRIMARY: xxx
```

### Check via API
```bash
# Get instance info
curl http://localhost:8080/stats

# Look for instance mode in response
```

## Best Practices

### Development
```properties
# Use standalone mode
cluster.enabled=false
```

### Production - High Availability
```properties
# Run at least 2 instances
# Primary:
cluster.enabled=true
cluster.mode=primary

# Secondary:
cluster.enabled=true
cluster.mode=secondary
cluster.auto.promote.on.no.primary=true
```

### Production - Single Instance
```properties
# If only one instance, disable clustering
cluster.enabled=false
```

## Troubleshooting

### Problem: Instance stays SECONDARY when it should be PRIMARY

**Solution 1:** Disable clustering for single instance
```properties
cluster.enabled=false
```

**Solution 2:** Wait for auto-promotion (10 seconds)
```bash
# Check logs for:
"No primary detected after 10000 ms. Auto-promoting to PRIMARY."
```

**Solution 3:** Start as PRIMARY explicitly
```bash
java -jar redisclone-1.0.0.jar --cluster.mode=primary
```

### Problem: Both instances become PRIMARY

**Cause:** Network partition or Kafka issues

**Solution:**
- Check Kafka connectivity
- Ensure both instances connect to same Kafka
- Check instance IDs are unique

### Problem: Secondary won't promote on primary failure

**Check:**
```properties
# Ensure auto-promotion is enabled
cluster.auto.promote.on.no.primary=true

# Ensure failover timeout is reasonable
cluster.failover.timeout.ms=15000
```

## Configuration Summary

| Setting | Default | Description |
|---------|---------|-------------|
| `cluster.enabled` | false | Enable/disable clustering |
| `cluster.mode` | primary | Initial mode (primary/secondary) |
| `cluster.auto.promote.on.no.primary` | true | Auto-promote if no primary |
| `cluster.startup.wait.for.primary.ms` | 10000 | Wait time for primary detection |
| `cluster.health.check.interval.ms` | 5000 | Heartbeat interval |
| `cluster.failover.timeout.ms` | 15000 | Failover trigger timeout |

## Quick Start Commands

**Single Instance (Recommended for most cases):**
```bash
java -jar redisclone-1.0.0.jar
```

**Clustered Setup:**
```bash
# Primary
java -jar redisclone-1.0.0.jar \
  --cluster.enabled=true \
  --cluster.mode=primary

# Secondary  
java -jar redisclone-1.0.0.jar \
  --cluster.enabled=true \
  --cluster.mode=secondary \
  --server.port=8081 \
  --network.server.port=6380
```