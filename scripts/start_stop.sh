#!/bin/bash
# start.sh - Linux/Mac startup script

echo "========================================="
echo "  AshRedis Clone Startup Script"
echo "========================================="
echo ""

# Configuration
JAR_FILE="target/redisclone-1.0.0.jar"
MODE="${1:-primary}"
HTTP_PORT="${2:-8080}"
NETWORK_PORT="${3:-6379}"

# Java options
JAVA_OPTS="-Xmx2g -Xms512m -XX:+UseG1GC"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR file not found: $JAR_FILE"
    echo "Please run: mvn clean package"
    exit 1
fi

echo "Starting AshRedis in $MODE mode..."
echo "HTTP Port: $HTTP_PORT"
echo "Network Port: $NETWORK_PORT"
echo ""

# Start application
java $JAVA_OPTS \
    -Dcluster.mode=$MODE \
    -Dserver.port=$HTTP_PORT \
    -Dnetwork.server.port=$NETWORK_PORT \
    -jar $JAR_FILE

# ============================================
# start.bat - Windows startup script
# ============================================

@echo off
REM start.bat - Windows startup script

echo =========================================
echo   AshRedis Clone Startup Script
echo =========================================
echo.

REM Configuration
set JAR_FILE=target\redisclone-1.0.0.jar
set MODE=%1
if "%MODE%"=="" set MODE=primary
set HTTP_PORT=%2
if "%HTTP_PORT%"=="" set HTTP_PORT=8080
set NETWORK_PORT=%3
if "%NETWORK_PORT%"=="" set NETWORK_PORT=6379

REM Java options
set JAVA_OPTS=-Xmx2g -Xms512m -XX:+UseG1GC

REM Check if JAR exists
if not exist "%JAR_FILE%" (
    echo ERROR: JAR file not found: %JAR_FILE%
    echo Please run: mvn clean package
    exit /b 1
)

echo Starting AshRedis in %MODE% mode...
echo HTTP Port: %HTTP_PORT%
echo Network Port: %NETWORK_PORT%
echo.

REM Start application
java %JAVA_OPTS% ^
    -Dcluster.mode=%MODE% ^
    -Dserver.port=%HTTP_PORT% ^
    -Dnetwork.server.port=%NETWORK_PORT% ^
    -jar %JAR_FILE%

REM ============================================
REM Usage Examples
REM ============================================

REM Linux/Mac:
REM # Start primary instance (default)
REM ./start.sh
REM
REM # Start primary on custom ports
REM ./start.sh primary 8080 6379
REM
REM # Start secondary instance
REM ./start.sh secondary 8081 6380

REM Windows:
REM # Start primary instance (default)
REM start.bat
REM
REM # Start primary on custom ports
REM start.bat primary 8080 6379
REM
REM # Start secondary instance
REM start.bat secondary 8081 6380

# ============================================
# run-cluster.sh - Start complete cluster
# ============================================

#!/bin/bash
# run-cluster.sh - Start primary and secondary instances

echo "Starting AshRedis Cluster..."
echo ""

# Start primary in background
echo "Starting PRIMARY instance on ports 8080/6379..."
java -Xmx2g -Xms512m \
    -Dcluster.mode=primary \
    -Dserver.port=8080 \
    -Dnetwork.server.port=6379 \
    -jar target/redisclone-1.0.0.jar > logs/primary.log 2>&1 &
PRIMARY_PID=$!
echo "Primary PID: $PRIMARY_PID"

# Wait for primary to start
sleep 10

# Start secondary in background
echo "Starting SECONDARY instance on ports 8081/6380..."
java -Xmx2g -Xms512m \
    -Dcluster.mode=secondary \
    -Dserver.port=8081 \
    -Dnetwork.server.port=6380 \
    -jar target/redisclone-1.0.0.jar > logs/secondary.log 2>&1 &
SECONDARY_PID=$!
echo "Secondary PID: $SECONDARY_PID"

echo ""
echo "========================================="
echo "Cluster started successfully!"
echo "========================================="
echo ""
echo "Primary Web UI:   http://localhost:8080"
echo "Primary Network:  localhost:6379"
echo ""
echo "Secondary Web UI:   http://localhost:8081"
echo "Secondary Network:  localhost:6380"
echo ""
echo "To stop cluster:"
echo "  kill $PRIMARY_PID $SECONDARY_PID"
echo ""
echo "View logs:"
echo "  tail -f logs/primary.log"
echo "  tail -f logs/secondary.log"
echo ""

# Save PIDs to file
echo $PRIMARY_PID > .primary.pid
echo $SECONDARY_PID > .secondary.pid

# ============================================
# stop-cluster.sh - Stop cluster
# ============================================

#!/bin/bash
# stop-cluster.sh - Stop all instances

echo "Stopping AshRedis Cluster..."

if [ -f .primary.pid ]; then
    PRIMARY_PID=$(cat .primary.pid)
    echo "Stopping primary (PID: $PRIMARY_PID)..."
    kill $PRIMARY_PID 2>/dev/null
    rm .primary.pid
fi

if [ -f .secondary.pid ]; then
    SECONDARY_PID=$(cat .secondary.pid)
    echo "Stopping secondary (PID: $SECONDARY_PID)..."
    kill $SECONDARY_PID 2>/dev/null
    rm .secondary.pid
fi

echo "Cluster stopped."

# ============================================
# test-connection.sh - Test connectivity
# ============================================

#!/bin/bash
# test-connection.sh - Test AshRedis connectivity

echo "Testing AshRedis Connection..."
echo ""

# Test HTTP (Web UI)
echo "1. Testing Web UI (HTTP)..."
HTTP_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080)
if [ "$HTTP_RESPONSE" = "200" ] || [ "$HTTP_RESPONSE" = "302" ]; then
    echo "   ✓ Web UI is accessible"
else
    echo "   ✗ Web UI is not accessible (HTTP $HTTP_RESPONSE)"
fi

echo ""

# Test Network Server
echo "2. Testing Network Server..."
if command -v nc &> /dev/null; then
    echo "PING" | nc localhost 6379 -w 1 > /tmp/redis-test 2>&1
    if grep -q "PONG" /tmp/redis-test; then
        echo "   ✓ Network server is responding"
    else
        echo "   ✗ Network server is not responding"
    fi
    rm -f /tmp/redis-test
else
    echo "   ! netcat (nc) not found, skipping network test"
fi

echo ""
echo "Connection test complete."

# ============================================
# Make scripts executable:
# chmod +x start.sh run-cluster.sh stop-cluster.sh test-connection.sh
# ============================================