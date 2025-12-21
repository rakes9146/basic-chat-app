# Troubleshooting Online Status & Message Delivery Issues

## Problems
1. ❌ Online status not displaying (users show offline)
2. ❌ Message delivery delayed or not real-time

## Root Causes & Fixes

### Issue 1: WebSocket Connection Not Established

**Symptoms**:
- Online status always shows offline
- Messages don't appear in real-time
- Need to refresh page to see new messages

**Diagnosis**:
```javascript
// Check browser console for WebSocket errors:
WebSocket connection to 'ws://54.154.129.84/ws' failed
```

**Causes**:
1. Firewall blocking WebSocket connections
2. Nginx WebSocket proxy not configured correctly
3. Message service not accessible from frontend

**Fix Steps**:

#### 1. Update Instance 2 Firewall (CRITICAL)

Go to **AWS Lightsail Console** → **Instance 2 (35.153.96.103)** → **Networking** → **Firewall**

**Add This Rule**:
```
Application: Custom
Protocol: TCP
Port: 8083
Source: 54.154.129.84/32
Description: Allow WebSocket from Frontend
```

This allows nginx on Instance 6 to proxy WebSocket connections to message-service on Instance 2.

#### 2. Verify Nginx WebSocket Proxy

On Instance 6, check nginx configuration:
```bash
ssh ec2-user@54.154.129.84
docker exec chat-frontend cat /etc/nginx/conf.d/default.conf | grep -A 10 "location /ws"
```

Should show:
```nginx
location /ws/ {
    proxy_pass http://35.153.96.103:8083/ws/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
```

#### 3. Test WebSocket Connection

From your browser console:
```javascript
const ws = new WebSocket('ws://54.154.129.84/ws');
ws.onopen = () => console.log('✅ WebSocket connected!');
ws.onerror = (err) => console.error('❌ WebSocket error:', err);
ws.onclose = () => console.log('❌ WebSocket closed');
```

---

### Issue 2: Redis Connection Issues (Online Status)

**Symptoms**:
- Online/offline status not updating
- Presence information not tracked

**Diagnosis**:

Check if message-service can reach Redis:
```bash
# SSH to Instance 2
ssh ec2-user@35.153.96.103

# Check message-service logs for Redis errors
docker-compose logs message-service | grep -i redis

# Test Redis connection from message-service container
docker exec chat-message-service redis-cli -h 98.89.238.241 -p 6379 ping
```

**Expected**: `PONG`

**If fails**: Redis firewall needs updating.

#### Update Instance 5 Firewall (Redis)

Go to **AWS Lightsail Console** → **Instance 5 (98.89.238.241)** → **Networking** → **Firewall**

**Current rules should have**:
```
Application: Custom
Protocol: TCP
Port: 6379
Source: 35.153.96.103/32
```

This allows Instance 2 (services) to access Redis.

---

### Issue 3: Kafka Message Delays

**Symptoms**:
- Messages sent but arrive after delay
- Messages not appearing in correct order

**Diagnosis**:

Check Kafka connection from message-service:
```bash
# SSH to Instance 2
ssh ec2-user@35.153.96.103

# Check message-service logs for Kafka errors
docker-compose logs message-service | grep -i kafka

# Check if Kafka is accessible
docker exec chat-message-service bash -c "echo 'test' | nc -w 2 3.147.109.193 9092"
```

**If connection fails**: Kafka firewall needs updating.

#### Update Instance 4 Firewall (Kafka)

Go to **AWS Lightsail Console** → **Instance 4 (3.147.109.193)** → **Networking** → **Firewall**

**Should have**:
```
Application: Custom
Protocol: TCP
Port: 9092
Source: 35.153.96.103/32
```

---

## Quick Fix Checklist

### 1. Update All Firewall Rules

**Instance 2 (35.153.96.103) - User & Message Services**:
```
✅ Port 8081 from 54.217.247.163/32 (Gateway)
✅ Port 8083 from 54.217.247.163/32 (Gateway)
⭐ Port 8083 from 54.154.129.84/32 (Frontend - WebSocket) ← ADD THIS!
✅ Port 22 from Anywhere
```

**Instance 4 (3.147.109.193) - Kafka**:
```
✅ Port 9092 from 35.153.96.103/32 (Services)
✅ Port 22 from Anywhere
```

**Instance 5 (98.89.238.241) - Redis**:
```
✅ Port 6379 from 35.153.96.103/32 (Services)
✅ Port 22 from Anywhere
```

### 2. Test Connections

```bash
# Test Redis from Instance 2
ssh ec2-user@35.153.96.103
docker exec chat-message-service redis-cli -h 98.89.238.241 ping

# Test Kafka from Instance 2
docker exec chat-message-service bash -c "timeout 2 nc -zv 3.147.109.193 9092"

# Test WebSocket endpoint from Instance 6
ssh ec2-user@54.154.129.84
curl -I http://35.153.96.103:8083/ws
```

### 3. Restart Message Service

After firewall updates:
```bash
ssh ec2-user@35.153.96.103
docker-compose restart message-service
docker-compose logs -f message-service
```

Look for:
```
✅ Connected to Redis at 98.89.238.241:6379
✅ Connected to Kafka at 3.147.109.193:9092
✅ WebSocket endpoint registered at /ws
```

### 4. Test in Browser

1. Open: http://54.154.129.84
2. Login with your user
3. Open Browser Console (F12)
4. Look for:
   ```
   ✅ WebSocket connection established
   ✅ User online status: true
   ```

---

## Verification Commands

### Check WebSocket from Browser

```javascript
// Paste in browser console
fetch('http://54.154.129.84/MESSAGE-SERVICE/actuator/health')
  .then(r => r.json())
  .then(d => console.log('Message Service:', d));
```

### Check Online Status

```bash
# SSH to Instance 5 (Redis)
ssh ec2-user@98.89.238.241

# Check online users
docker exec -it redis redis-cli KEYS "user:*:online"
docker exec -it redis redis-cli GET "user:123:online"  # Replace 123 with actual user ID
```

### Monitor Real-Time Messages

```bash
# SSH to Instance 4 (Kafka)
ssh ec2-user@3.147.109.193

# Watch messages topic
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic chat-messages \
  --from-beginning
```

---

## Advanced Debugging

### Message Service Logs

```bash
ssh ec2-user@35.153.96.103

# Full logs
docker-compose logs -f message-service

# Filter for WebSocket
docker-compose logs message-service | grep -i websocket

# Filter for Redis
docker-compose logs message-service | grep -i redis

# Filter for Kafka
docker-compose logs message-service | grep -i kafka
```

### Network Connectivity Test

```bash
# From Instance 2, test all connections
ssh ec2-user@35.153.96.103

# Test MySQL
docker exec chat-message-service bash -c "nc -zv 3.147.141.101 3306"

# Test Kafka
docker exec chat-message-service bash -c "nc -zv 3.147.109.193 9092"

# Test Redis
docker exec chat-message-service bash -c "nc -zv 98.89.238.241 6379"

# Test Gateway
docker exec chat-message-service bash -c "nc -zv 54.217.247.163 8761"
```

---

## Performance Optimization

### 1. Increase WebSocket Timeouts

If connections drop frequently, on Instance 6:

Edit nginx-aws.conf and increase timeouts:
```nginx
location /ws/ {
    proxy_read_timeout 7200;  # 2 hours
    proxy_send_timeout 7200;
    proxy_connect_timeout 120s;
}
```

### 2. Redis Connection Pooling

Check message-service application.properties:
```properties
spring.redis.lettuce.pool.max-active=20
spring.redis.lettuce.pool.max-idle=10
spring.redis.lettuce.pool.min-idle=5
spring.redis.timeout=60000
```

### 3. Kafka Consumer Configuration

Check message-service application.properties:
```properties
spring.kafka.consumer.enable-auto-commit=true
spring.kafka.consumer.auto-commit-interval=1000
spring.kafka.consumer.max-poll-records=50
```

---

## Expected Behavior After Fixes

✅ **Online Status**:
- User appears online immediately after login
- Other users see status change in real-time
- Status persists in Redis

✅ **Message Delivery**:
- Messages appear instantly (< 1 second)
- No refresh needed
- Messages persist in MySQL
- Message events published to Kafka

✅ **WebSocket**:
- Connection established on page load
- Stays connected during session
- Reconnects automatically if dropped

---

## Summary

**Most Likely Issue**: Missing firewall rule on Instance 2 port 8083 from Instance 6.

**Quick Fix**:
1. Add firewall rule: Instance 2, Port 8083, Source: 54.154.129.84/32
2. Restart message-service: `docker-compose restart message-service`
3. Hard refresh browser: Ctrl+Shift+R
4. Test online status and send a message

This should resolve both the online status display and message delivery delay issues!
