# AshRedis Clone - Project Structure

## Directory Layout

```
ashredisclone/
├── pom.xml
├── README.md
├── QUICKSTART.md
├── Dockerfile
├── docker-compose.yml
├── start.sh
├── start.bat
├── run-cluster.sh
├── stop-cluster.sh
├── test-connection.sh
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ash/projects/redisclone/
│   │   │       ├── RedisCloneApplication.java
│   │   │       │
│   │   │       ├── model/
│   │   │       │   ├── CacheEntry.java
│   │   │       │   ├── DataType.java
│   │   │       │   ├── Region.java
│   │   │       │   ├── User.java
│   │   │       │   ├── SortedSetEntry.java
│   │   │       │   └── ReplicationEvent.java
│   │   │       │
│   │   │       ├── service/
│   │   │       │   ├── CacheService.java
│   │   │       │   ├── ExtendedCacheService.java
│   │   │       │   ├── RedisCommandService.java
│   │   │       │   ├── ReplicationService.java
│   │   │       │   ├── PubSubService.java
│   │   │       │   ├── AsyncPubService.java
│   │   │       │   ├── TransactionService.java
│   │   │       │   └── UserService.java
│   │   │       │
│   │   │       ├── repository/
│   │   │       │   └── CacheRepository.java
│   │   │       │
│   │   │       ├── controller/
│   │   │       │   └── WebController.java
│   │   │       │
│   │   │       ├── network/
│   │   │       │   └── NetworkServer.java
│   │   │       │
│   │   │       └── client/
│   │   │           └── AshRedisClient.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── users.json
│   │       │
│   │       └── templates/
│   │           ├── base.html
│   │           ├── login.html
│   │           ├── index.html
│   │           ├── regions.html
│   │           ├── region-detail.html
│   │           ├── entry-detail.html
│   │           ├── stats.html
│   │           └── error.html
│   │
│   └── test/
│       └── java/
│           └── com/ash/projects/redisclone/
│               └── (test files)
│
├── target/
│   └── redisclone-1.0.0.jar
│
└── logs/
    ├── primary.log
    └── secondary.log
```

## Critical Files Location

### Java Source Files
All Java files must be in: `src/main/java/com/ash/projects/redisclone/`

### HTML Templates
All HTML templates must be in: `src/main/resources/templates/`
- base.html
- login.html
- index.html
- regions.html
- region-detail.html
- entry-detail.html
- stats.html
- error.html

### Configuration Files
Must be in: `src/main/resources/`
- application.properties
- users.json

### Python Client
Place separately or in a `clients/` directory:
- ashredis_client.py

## Setting Up the Project

### Step 1: Create Directory Structure
```bash
mkdir -p src/main/java/com/ash/projects/redisclone/{model,service,repository,controller,network,client}
mkdir -p src/main/resources/templates
mkdir -p src/test/java/com/ash/projects/redisclone
mkdir -p logs
```

### Step 2: Place Files
1. Copy all `.java` files to their respective packages
2. Copy all `.html` files to `src/main/resources/templates/`
3. Copy `application.properties` and `users.json` to `src/main/resources/`
4. Place `pom.xml` in root directory

### Step 3: Build
```bash
mvn clean package
```

### Step 4: Run
```bash
java -jar target/redisclone-1.0.0.jar
```

## Common Issues

### Issue: Templates not found
**Solution:** Ensure all HTML files are in `src/main/resources/templates/`

### Issue: application.properties not loaded
**Solution:** Ensure file is in `src/main/resources/` (not in templates folder)

### Issue: users.json not found
**Solution:** Ensure file is in `src/main/resources/` (same location as application.properties)

### Issue: Compilation errors
**Solution:** Ensure all Java files are in correct package structure

## Verification

After setting up, verify structure:
```bash
# Check templates
ls -la src/main/resources/templates/

# Should show:
# base.html
# login.html
# index.html
# regions.html
# region-detail.html
# entry-detail.html
# stats.html
# error.html

# Check resources
ls -la src/main/resources/

# Should show:
# application.properties
# users.json
# templates/ (directory)
```

## Maven Build Output
After successful build, you should see:
```
target/
├── classes/
│   ├── com/ash/projects/redisclone/ (compiled classes)
│   ├── templates/ (copied HTML files)
│   ├── application.properties
│   └── users.json
└── redisclone-1.0.0.jar (fat JAR)
```

The fat JAR includes everything needed to run the application.