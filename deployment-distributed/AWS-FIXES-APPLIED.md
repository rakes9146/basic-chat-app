# AWS Deployment Fixes Applied

## Summary
This document lists all fixes applied to ensure the distributed deployment works correctly on AWS Lightsail, based on issues discovered during local testing.

## Issues Fixed

### 1. ✅ Message Service - Random Port Issue
**Problem**: `message-service/src/main/resources/application.properties` had `server.port=0` causing random port assignment.

**Fix Applied**:
- Changed `server.port=0` to `server.port=8083`
- Added `SERVER_PORT=8083` environment variable in `instance-2-user-message-services/docker-compose.yml`

**Impact**: Message service will now consistently run on port 8083 instead of random ports like 39945.

---

### 2. ✅ Message Service - Wrong Eureka Port
**Problem**: `message-service/src/main/resources/application.properties` had Eureka URL pointing to port 8123.

**Fix Applied**:
- Changed `eureka.client.service-url.defaultZone=http://localhost:8123/eureka` to `http://localhost:8761/eureka`

**Impact**: Message service will correctly connect to Eureka on the standard port 8761.

---

### 3. ✅ Hardcoded Localhost Hostnames
**Problem**: Services had `eureka.instance.hostname=localhost` hardcoded in application.properties.

**Fix Applied**:
- Removed `eureka.instance.hostname=localhost` from:
  - `eurekaserver/src/main/resources/application.properties`
  - `message-service/src/main/resources/application.properties`
  - `user-service/src/main/resources/application.properties`
- Changed to `eureka.instance.prefer-ip-address=true`

**Impact**: Services will now use their actual IP addresses for Eureka registration instead of "localhost", allowing proper service discovery across instances.

---

### 4. ✅ MySQL Connection - Public Key Retrieval
**Problem**: MySQL 8.0 requires explicit public key retrieval permission.

**Fix Status**: Already implemented in AWS deployment files.
- Both user-service and message-service have `allowPublicKeyRetrieval=true` in their JDBC URLs in `instance-2-user-message-services/docker-compose.yml`

---

### 5. ✅ Explicit Port Configuration
**Problem**: Environment variables weren't guaranteed to override application.properties.

**Fix Applied**:
- Added explicit `SERVER_PORT=8081` to user-service environment variables
- Added explicit `SERVER_PORT=8083` to message-service environment variables

**Impact**: Services will use correct ports even if application.properties defaults are present.

---

## Verification Checklist

Before deploying to AWS, ensure:

- [ ] **Rebuild Docker Images**: All three services need to be rebuilt with corrected application.properties:
  ```bash
  # Message Service
  cd message-service
  mvn clean package -DskipTests
  docker build -t rakes9146/chat-message-service:distributed .
  
  # Eureka Server
  cd eurekaserver
  mvn clean package -DskipTests
  docker build -t rakes9146/chat-eureka:distributed .
  
  # User Service (already fixed)
  cd user-service
  mvn clean package -DskipTests
  docker build -t rakes9146/chat-user-service:distributed .
  ```

- [ ] **Push Updated Images to Docker Hub**:
  ```bash
  docker push rakes9146/chat-eureka:distributed
  docker push rakes9146/chat-user-service:distributed
  docker push rakes9146/chat-message-service:distributed
  ```

- [ ] **Verify AWS docker-compose.yml files** in:
  - `instance-1-eureka-gateway/docker-compose.yml`
  - `instance-2-user-message-services/docker-compose.yml`

- [ ] **Test locally first** with `docker-compose-local.yml` before AWS deployment

---

## Configuration Summary

### Instance 1 - Eureka + Gateway
**File**: `instance-1-eureka-gateway/docker-compose.yml`
- Eureka: Port 8761, no hostname hardcoding ✅
- Gateway: Port 8082, connects to Eureka at 54.217.247.163:8761 ✅
- JVM: -Xmx256m -Xms128m for both services ✅

### Instance 2 - User + Message Services
**File**: `instance-2-user-message-services/docker-compose.yml`
- User Service: Port 8081 (explicitly set via SERVER_PORT) ✅
- Message Service: Port 8083 (explicitly set via SERVER_PORT) ✅
- Both connect to Eureka at 54.217.247.163:8761 ✅
- MySQL connections include `allowPublicKeyRetrieval=true` ✅
- JVM: -Xmx512m -Xms256m for both services ✅

### Instance 3 - MySQL
- Database: chatdb
- User: chatuser / chatpass123
- Port: 3306
- Memory: 1GB instance ✅

### Instance 4 - Kafka
- Broker: Port 9092
- KRaft mode (no Zookeeper)
- Memory: 2GB instance ✅

### Instance 5 - Redis
- Port: 6379
- Memory: 512MB instance ✅

### Instance 6 - Frontend
- Nginx + Angular
- Port: 80
- Proxies to Gateway at 54.217.247.163:8082 ✅

---

## Post-Deployment Testing

1. **Verify Eureka Dashboard**:
   ```
   http://54.217.247.163:8761
   ```
   - Should show USER-SERVICE and MESSAGE-SERVICE registered
   - Should show correct hostnames (35.153.96.103) not localhost
   - Should show correct ports (8081, 8083) not random ports

2. **Test API Gateway**:
   ```bash
   curl http://54.217.247.163:8082/USER-SERVICE/actuator/health
   curl http://54.217.247.163:8082/MESSAGE-SERVICE/actuator/health
   ```
   - Should return 200 OK, not 500 errors

3. **Test User Registration**:
   ```bash
   curl -X POST http://54.217.247.163:8082/USER-SERVICE/api/users/register \
     -H "Content-Type: application/json" \
     -d '{"username":"test","email":"test@test.com","password":"Test123"}'
   ```
   - Should successfully create user

4. **Verify MySQL Data**:
   ```sql
   USE chatdb;
   SELECT * FROM users;
   ```
   - Should see registered users

---

## Rollback Plan

If issues occur after deployment:

1. **Check service logs**:
   ```bash
   docker logs chat-user-service
   docker logs chat-message-service
   docker logs chat-eureka
   docker logs chat-gateway
   ```

2. **Verify Eureka registration**:
   ```bash
   curl http://54.217.247.163:8761/eureka/apps
   ```

3. **Rollback to previous images** (if necessary):
   - Keep backup of working images tagged with `:backup` or `:v1.0`

---

## Related Documentation

- [LOCAL-BUILD-AND-TEST.md](LOCAL-BUILD-AND-TEST.md) - Local testing procedures
- [AWS-DEPLOYMENT-COMPLETE.md](AWS-DEPLOYMENT-COMPLETE.md) - Full AWS deployment guide
- [FIXES-APPLIED.md](FIXES-APPLIED.md) - Local deployment fixes
