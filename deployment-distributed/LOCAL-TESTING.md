# 🖥️ Local Testing Guide - Distributed Deployment

## Test Distributed Architecture on Your Local Machine

Before deploying to AWS Lightsail, test everything locally to ensure all images work correctly.

---

## 📋 Prerequisites

- Docker Desktop running
- Maven 3.9+
- Node.js 20+
- At least 8GB RAM available
- Ports available: 8761, 8082, 8081, 8083, 3306, 9092, 6379, 80

---

## 🏗️ Step 1: Build All JAR Files

### Build Spring Boot Services

```powershell
# Navigate to project root
cd "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app"

# Build Eureka Server
cd eurekaserver
mvn clean package -DskipTests
cd ..

# Build API Gateway
cd apigateway
mvn clean package -DskipTests
cd ..

# Build User Service
cd user-service
mvn clean package -DskipTests
cd ..

# Build Message Service
cd message-service
mvn clean package -DskipTests
cd ..
```

### Build Frontend

```powershell
# Build Angular app
cd frontend
npm install
npm run build --configuration production
cd ..
```

**Expected Output**: 
- `eurekaserver/target/*.jar`
- `apigateway/target/*.jar`
- `user-service/target/*.jar`
- `message-service/target/*.jar`
- `frontend/dist/frontend/browser/`

---

## 🐳 Step 2: Build Docker Images Locally

```powershell
# Navigate to deployment folder
cd deployment-distributed

# Build Eureka Server
cd instance-1-eureka-gateway
docker build -f Dockerfile.eureka -t rakes9146/chat-eureka:distributed ../../
echo "✅ Eureka image built"

# Build API Gateway
docker build -f Dockerfile.gateway -t rakes9146/chat-api-gateway:distributed ../../
echo "✅ Gateway image built"

# Build User Service
cd ../instance-2-user-message-services
docker build -f Dockerfile.user -t rakes9146/chat-user-service:distributed ../../
echo "✅ User Service image built"

# Build Message Service
docker build -f Dockerfile.message -t rakes9146/chat-message-service:distributed ../../
echo "✅ Message Service image built"

# Build Frontend
cd ../instance-6-frontend
docker build -t rakes9146/chat-frontend:distributed ../../frontend
echo "✅ Frontend image built"

cd ../..
```

**Verify Images Built**:
```powershell
docker images | Select-String "rakes9146/chat"
```

**Expected Output**:
```
rakes9146/chat-eureka:distributed
rakes9146/chat-api-gateway:distributed
rakes9146/chat-user-service:distributed
rakes9146/chat-message-service:distributed
rakes9146/chat-frontend:distributed
```

---

## 🚀 Step 3: Create Local Docker Compose File

Create `deployment-distributed/docker-compose-local.yml`:

```powershell
# Create local testing compose file
cd deployment-distributed
New-Item -ItemType File -Path "docker-compose-local.yml" -Force
```

```yaml
# deployment-distributed/docker-compose-local.yml
version: '3.8'

services:
  # ===== INFRASTRUCTURE =====
  mysql:
    image: mysql:8.0
    container_name: chat-mysql-local
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=rootpass123
      - MYSQL_DATABASE=chatdb
      - MYSQL_USER=chatuser
      - MYSQL_PASSWORD=chatpass123
    volumes:
      - mysql-data-local:/var/lib/mysql
    networks:
      - chat-network-local
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka:
    image: apache/kafka:3.7.0
    container_name: chat-kafka-local
    ports:
      - "9092:9092"
    environment:
      - KAFKA_PROCESS_ROLES=broker,controller
      - KAFKA_NODE_ID=1
      - KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
      - KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER
      - KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092
      - KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      - KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT
      - KAFKA_LOG_DIRS=/var/lib/kafka/data
      - KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
      - KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1
      - KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1
    volumes:
      - kafka-data-local:/var/lib/kafka/data
    networks:
      - chat-network-local
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:9092 || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: chat-redis-local
    ports:
      - "6379:6379"
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data-local:/data
    networks:
      - chat-network-local
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ===== SERVICE DISCOVERY =====
  eureka:
    image: rakes9146/chat-eureka:distributed
    container_name: chat-eureka-local
    ports:
      - "8761:8761"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_REGISTER_WITH_EUREKA=false
      - EUREKA_CLIENT_FETCH_REGISTRY=false
      - JAVA_OPTS=-Xmx256m -Xms128m -XX:+UseG1GC
    networks:
      - chat-network-local
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:8761/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ===== API GATEWAY =====
  gateway:
    image: rakes9146/chat-api-gateway:distributed
    container_name: chat-gateway-local
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka/
      - CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:80
      - JAVA_OPTS=-Xmx256m -Xms128m -XX:+UseG1GC
    networks:
      - chat-network-local
    depends_on:
      eureka:
        condition: service_healthy

  # ===== USER SERVICE =====
  user-service:
    image: rakes9146/chat-user-service:distributed
    container_name: chat-user-service-local
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka/
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/chatdb?useSSL=false&serverTimezone=UTC
      - SPRING_DATASOURCE_USERNAME=chatuser
      - SPRING_DATASOURCE_PASSWORD=chatpass123
      - JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC
    networks:
      - chat-network-local
    depends_on:
      mysql:
        condition: service_healthy
      eureka:
        condition: service_healthy

  # ===== MESSAGE SERVICE =====
  message-service:
    image: rakes9146/chat-message-service:distributed
    container_name: chat-message-service-local
    ports:
      - "8083:8083"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka/
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/chatdb?useSSL=false&serverTimezone=UTC
      - SPRING_DATASOURCE_USERNAME=chatuser
      - SPRING_DATASOURCE_PASSWORD=chatpass123
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - WEBSOCKET_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:80
      - JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseG1GC
    networks:
      - chat-network-local
    depends_on:
      mysql:
        condition: service_healthy
      kafka:
        condition: service_healthy
      redis:
        condition: service_healthy
      eureka:
        condition: service_healthy

  # ===== FRONTEND =====
  frontend:
    image: rakes9146/chat-frontend:distributed
    container_name: chat-frontend-local
    ports:
      - "80:80"
    environment:
      - API_GATEWAY_URL=http://localhost:8082
      - WEBSOCKET_URL=ws://localhost:8083/ws
    networks:
      - chat-network-local
    depends_on:
      - gateway

volumes:
  mysql-data-local:
  kafka-data-local:
  redis-data-local:

networks:
  chat-network-local:
    driver: bridge
```

---

## 🎬 Step 4: Start All Services Locally

### Start Infrastructure First

```powershell
# Start MySQL, Kafka, Redis
docker-compose -f docker-compose-local.yml up -d mysql kafka redis

# Wait 30 seconds for infrastructure to be ready
Start-Sleep -Seconds 30

# Check infrastructure health
docker-compose -f docker-compose-local.yml ps
```

### Start Application Services

```powershell
# Start Eureka
docker-compose -f docker-compose-local.yml up -d eureka

# Wait for Eureka (20 seconds)
Start-Sleep -Seconds 20

# Start Gateway and Services
docker-compose -f docker-compose-local.yml up -d gateway user-service message-service

# Wait for services to register (30 seconds)
Start-Sleep -Seconds 30

# Start Frontend
docker-compose -f docker-compose-local.yml up -d frontend
```

### Or Start Everything at Once

```powershell
# Start all services
docker-compose -f docker-compose-local.yml up -d

# Watch logs
docker-compose -f docker-compose-local.yml logs -f
```

---

## ✅ Step 5: Verify Local Deployment

### Check All Containers Running

```powershell
docker-compose -f docker-compose-local.yml ps
```

**Expected Output**: All services should be "Up" and healthy.

### Check Eureka Dashboard

Open browser: http://localhost:8761

**Expected**: Should see registered services:
- API-GATEWAY
- USER-SERVICE
- MESSAGE-SERVICE

### Test API Gateway

```powershell
# Health check
curl http://localhost:8082/actuator/health

# Test routing to User Service
curl http://localhost:8082/user-service/actuator/health

# Test routing to Message Service
curl http://localhost:8082/message-service/actuator/health
```

### Test Frontend

Open browser: http://localhost

**Expected**: Angular application loads successfully

### Check Logs

```powershell
# All services
docker-compose -f docker-compose-local.yml logs

# Specific service
docker-compose -f docker-compose-local.yml logs -f user-service
docker-compose -f docker-compose-local.yml logs -f message-service
docker-compose -f docker-compose-local.yml logs -f gateway
```

### Check Kafka Topics

```powershell
docker-compose -f docker-compose-local.yml exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

**Expected Topics**:
- chat.message.sent
- chat.message.delivered
- chat.message.read
- chat.user.presence

### Check Redis

```powershell
docker-compose -f docker-compose-local.yml exec redis redis-cli ping
```

**Expected**: PONG

---

## 🧪 Step 6: Test Application Flow

### 1. Register User

Open browser: http://localhost

- Click "Register"
- Fill form
- Submit

### 2. Login

- Enter credentials
- Click "Login"
- Should redirect to chat page

### 3. Send Message

- Type message
- Click "Send"
- Message should appear

### 4. Check Database

```powershell
docker-compose -f docker-compose-local.yml exec mysql mysql -u chatuser -pchatpass123 chatdb -e "SELECT * FROM users;"
docker-compose -f docker-compose-local.yml exec mysql mysql -u chatuser -pchatpass123 chatdb -e "SELECT * FROM messages;"
```

---

## 🛑 Step 7: Stop Services

### Stop All Services

```powershell
docker-compose -f docker-compose-local.yml down
```

### Stop and Remove Volumes (Clean Slate)

```powershell
docker-compose -f docker-compose-local.yml down -v
```

---

## 📤 Step 8: Push Images to Docker Hub

After successful local testing, push images to Docker Hub:

```powershell
# Login to Docker Hub
docker login

# Push all images
docker push rakes9146/chat-eureka:distributed
docker push rakes9146/chat-api-gateway:distributed
docker push rakes9146/chat-user-service:distributed
docker push rakes9146/chat-message-service:distributed
docker push rakes9146/chat-frontend:distributed
```

---

## 🚀 Step 9: Deploy to AWS Lightsail

Now that everything works locally, deploy to AWS using the distributed architecture files.

See: [QUICK-START.md](QUICK-START.md) for AWS deployment steps.

---

## 🔧 Troubleshooting Local Testing

### Issue: Port Already in Use

**Error**: `Bind for 0.0.0.0:3306 failed: port is already allocated`

**Solution**:
```powershell
# Check what's using the port
netstat -ano | findstr :3306

# Stop local MySQL/services
# Or use different ports in docker-compose-local.yml
```

### Issue: Container Won't Start

**Solution**:
```powershell
# Check logs
docker-compose -f docker-compose-local.yml logs [service-name]

# Restart specific service
docker-compose -f docker-compose-local.yml restart [service-name]
```

### Issue: Service Not Registering with Eureka

**Solution**:
```powershell
# Check Eureka is running
curl http://localhost:8761/actuator/health

# Check service logs
docker-compose -f docker-compose-local.yml logs -f user-service

# Restart service
docker-compose -f docker-compose-local.yml restart user-service
```

### Issue: Out of Memory

**Solution**:
```powershell
# Check Docker Desktop settings
# Increase RAM allocation: Settings > Resources > Memory (8GB+)

# Or reduce JVM heap in docker-compose-local.yml:
# JAVA_OPTS=-Xmx256m -Xms128m
```

---

## 📊 Resource Usage Check

```powershell
# Check container resource usage
docker stats --no-stream
```

**Expected Memory Usage**:
- MySQL: ~400MB
- Kafka: ~1GB
- Redis: ~50MB
- Eureka: ~250MB
- Gateway: ~250MB
- User Service: ~500MB
- Message Service: ~500MB
- Frontend: ~50MB

**Total**: ~3GB

---

## ✅ Local Testing Checklist

- [ ] All JAR files built successfully
- [ ] All Docker images built successfully
- [ ] MySQL container running and healthy
- [ ] Kafka container running and healthy
- [ ] Redis container running and healthy
- [ ] Eureka container running and accessible
- [ ] Gateway container running and accessible
- [ ] User Service registered with Eureka
- [ ] Message Service registered with Eureka
- [ ] Frontend accessible at http://localhost
- [ ] User registration works
- [ ] User login works
- [ ] Message sending works
- [ ] WebSocket connection works
- [ ] Kafka topics created
- [ ] Redis presence tracking works
- [ ] All logs show no errors
- [ ] Images pushed to Docker Hub

---

## 🎯 Quick Command Summary

```powershell
# Build everything
cd "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app"
cd eurekaserver; mvn clean package -DskipTests; cd ..
cd apigateway; mvn clean package -DskipTests; cd ..
cd user-service; mvn clean package -DskipTests; cd ..
cd message-service; mvn clean package -DskipTests; cd ..
cd frontend; npm install; npm run build --configuration production; cd ..

# Build images
cd deployment-distributed/instance-1-eureka-gateway
docker build -f Dockerfile.eureka -t rakes9146/chat-eureka:distributed ../../
docker build -f Dockerfile.gateway -t rakes9146/chat-api-gateway:distributed ../../
cd ../instance-2-user-message-services
docker build -f Dockerfile.user -t rakes9146/chat-user-service:distributed ../../
docker build -f Dockerfile.message -t rakes9146/chat-message-service:distributed ../../
cd ../instance-6-frontend
docker build -t rakes9146/chat-frontend:distributed ../../frontend
cd ..

# Test locally
docker-compose -f docker-compose-local.yml up -d
docker-compose -f docker-compose-local.yml logs -f

# Verify
curl http://localhost:8761
curl http://localhost:8082/actuator/health
# Open http://localhost in browser

# Push to Docker Hub
docker push rakes9146/chat-eureka:distributed
docker push rakes9146/chat-api-gateway:distributed
docker push rakes9146/chat-user-service:distributed
docker push rakes9146/chat-message-service:distributed
docker push rakes9146/chat-frontend:distributed

# Clean up
docker-compose -f docker-compose-local.yml down -v
```

---

**Next Step**: After successful local testing, proceed with AWS Lightsail deployment using [QUICK-START.md](QUICK-START.md)
