# Distributed Microservices Deployment Guide
## AWS Lightsail Multi-Instance Architecture

**Last Updated**: December 15, 2025  
**Architecture**: 6 Separate Instances with Static IPs

---

## 📋 Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Instance Configuration](#instance-configuration)
4. [Deployment Steps](#deployment-steps)
5. [Verification & Testing](#verification--testing)
6. [Troubleshooting](#troubleshooting)
7. [Monitoring](#monitoring)
8. [DNS Migration Plan](#dns-migration-plan)

---

## 🏗️ Architecture Overview

### Instance Distribution

| Instance | RAM | Services | Static IP | Ports |
|----------|-----|----------|-----------|-------|
| **Instance 1** | 1GB | Eureka + API Gateway | 54.217.247.163 | 8761, 8082 |
| **Instance 2** | 2GB | User + Message Services | 35.153.96.103 | 8081, 8083 |
| **Instance 3** | 1GB | MySQL | 3.147.141.101 | 3306 |
| **Instance 4** | 2GB | Kafka | 3.147.109.193 | 9092 |
| **Instance 5** | 512MB | Redis | 98.89.238.241 | 6379 |
| **Instance 6** | 1GB | Frontend (Nginx) | 54.154.129.84 | 80, 443 |

### Service Communication Flow

```
User Browser (Public)
    ↓
Frontend (54.154.129.84:80) ← Public Access
    ↓
API Gateway (54.217.247.163:8082) ← Public Access
    ↓
    ├─→ User Service (35.153.96.103:8081) ← Internal Only
    └─→ Message Service (35.153.96.103:8083) ← Internal + WebSocket
         ├─→ MySQL (3.147.141.101:3306) ← Internal Only
         ├─→ Kafka (3.147.109.193:9092) ← Internal Only
         └─→ Redis (98.89.238.241:6379) ← Internal Only

All Services register with:
Eureka Server (54.217.247.163:8761) ← Optional Public
```

---

## ✅ Prerequisites

### On Your Local Machine
- Docker Desktop installed
- Docker Hub account (rakes9146)
- Git configured
- Maven 3.9+ (for building)
- Node.js 20+ (for frontend)

### On AWS Lightsail
- 6 instances created with specified RAM
- Amazon Linux 2023 OS on all instances
- Static IPs attached to each instance
- SSH key pairs configured

---

## ⚙️ Instance Configuration

### Step 1: Install Docker on All Instances

SSH into each instance and run:

```bash
# Update system
sudo dnf update -y

# Install Docker
sudo dnf install docker -y

# Start Docker service
sudo systemctl start docker
sudo systemctl enable docker

# Add ec2-user to docker group
sudo usermod -aG docker ec2-user

# Log out and back in for group changes to take effect
exit
```

### Step 2: Install Docker Compose on All Instances

```bash
# Download Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# Make executable
sudo chmod +x /usr/local/bin/docker-compose

# Verify installation
docker-compose --version
```

### Step 3: Configure Firewalls

Refer to `shared-config/FIREWALL-RULES.txt` for detailed rules.

**Quick Security Checklist**:
- ✅ MySQL (3306): Only from 35.153.96.103
- ✅ Kafka (9092): Only from 35.153.96.103
- ✅ Redis (6379): Only from 35.153.96.103
- ✅ API Gateway (8082): Public access (0.0.0.0/0)
- ✅ Frontend (80, 443): Public access (0.0.0.0/0)

---

## 🚀 Deployment Steps

### Phase 1: Build Docker Images Locally

```bash
# Clone repository
cd /path/to/basic-chat-app

# Build all Spring Boot services
cd eurekaserver
mvn clean package -DskipTests

cd ../apigateway
mvn clean package -DskipTests

cd ../user-service
mvn clean package -DskipTests

cd ../message-service
mvn clean package -DskipTests

# Build Frontend
cd ../frontend
npm install
npm run build --configuration production
```

### Phase 2: Build and Push Docker Images

```bash
cd deployment-distributed

# Instance 1: Eureka + Gateway
cd instance-1-eureka-gateway
docker build -f Dockerfile.eureka -t rakes9146/chat-eureka:distributed ../../
docker build -f Dockerfile.gateway -t rakes9146/chat-api-gateway:distributed ../../
docker push rakes9146/chat-eureka:distributed
docker push rakes9146/chat-api-gateway:distributed

# Instance 2: User + Message Services
cd ../instance-2-user-message-services
docker build -f Dockerfile.user -t rakes9146/chat-user-service:distributed ../../
docker build -f Dockerfile.message -t rakes9146/chat-message-service:distributed ../../
docker push rakes9146/chat-user-service:distributed
docker push rakes9146/chat-message-service:distributed

# Instance 6: Frontend
cd ../instance-6-frontend
docker build -t rakes9146/chat-frontend:distributed ../../frontend
docker push rakes9146/chat-frontend:distributed
```

### Phase 3: Deploy Infrastructure Services (MySQL, Kafka, Redis)

**Deploy in order**: MySQL → Kafka → Redis

#### Instance 3: MySQL

```bash
# SSH into MySQL instance
ssh -i your-key.pem ec2-user@3.147.141.101

# Create deployment directory
mkdir -p ~/chat-deployment
cd ~/chat-deployment

# Upload docker-compose.yml (use scp or git clone)
# Option 1: Git clone
git clone https://github.com/rakes9146/basic-chat-app.git
cd basic-chat-app/deployment-distributed/instance-3-mysql

# Option 2: SCP from local
# scp -i your-key.pem docker-compose.yml ec2-user@3.147.141.101:~/chat-deployment/

# Start MySQL
docker-compose up -d

# Verify
docker-compose logs -f mysql
docker-compose exec mysql mysql -u chatuser -pchatpass123 -e "SHOW DATABASES;"
```

#### Instance 4: Kafka

```bash
# SSH into Kafka instance
ssh -i your-key.pem ec2-user@3.147.109.193

# Setup and start
cd ~/chat-deployment/basic-chat-app/deployment-distributed/instance-4-kafka
docker-compose up -d

# Verify
docker-compose logs -f kafka
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

#### Instance 5: Redis

```bash
# SSH into Redis instance
ssh -i your-key.pem ec2-user@98.89.238.241

# Setup and start
cd ~/chat-deployment/basic-chat-app/deployment-distributed/instance-5-redis
docker-compose up -d

# Verify
docker-compose logs -f redis
docker-compose exec redis redis-cli ping
```

### Phase 4: Deploy Core Services

#### Instance 1: Eureka + Gateway

```bash
# SSH into Eureka/Gateway instance
ssh -i your-key.pem ec2-user@54.217.247.163

# Navigate to deployment folder
cd ~/chat-deployment/basic-chat-app/deployment-distributed/instance-1-eureka-gateway

# Modify docker-compose.yml to use pre-built images instead of building
# Remove build sections, keep only image references

# Start services
docker-compose up -d

# Verify
docker-compose logs -f
curl http://localhost:8761/actuator/health
curl http://localhost:8082/actuator/health
```

#### Instance 2: User + Message Services

```bash
# SSH into Services instance
ssh -i your-key.pem ec2-user@35.153.96.103

# Navigate to deployment folder
cd ~/chat-deployment/basic-chat-app/deployment-distributed/instance-2-user-message-services

# Start services
docker-compose up -d

# Verify
docker-compose logs -f user-service
docker-compose logs -f message-service
curl http://localhost:8081/actuator/health
curl http://localhost:8083/actuator/health

# Check Eureka registration
curl http://54.217.247.163:8761/eureka/apps
```

### Phase 5: Deploy Frontend

#### Instance 6: Frontend

```bash
# SSH into Frontend instance
ssh -i your-key.pem ec2-user@54.154.129.84

# Navigate to deployment folder
cd ~/chat-deployment/basic-chat-app/deployment-distributed/instance-6-frontend

# Start frontend
docker-compose up -d

# Verify
docker-compose logs -f frontend
curl http://localhost/
```

---

## ✔️ Verification & Testing

### 1. Check All Services Running

```bash
# Instance 1: Eureka + Gateway
ssh ec2-user@54.217.247.163 "docker ps"

# Instance 2: User + Message
ssh ec2-user@35.153.96.103 "docker ps"

# Instance 3: MySQL
ssh ec2-user@3.147.141.101 "docker ps"

# Instance 4: Kafka
ssh ec2-user@3.147.109.193 "docker ps"

# Instance 5: Redis
ssh ec2-user@98.89.238.241 "docker ps"

# Instance 6: Frontend
ssh ec2-user@54.154.129.84 "docker ps"
```

### 2. Verify Eureka Dashboard

Open browser: `http://54.217.247.163:8761`

Expected registered services:
- API-GATEWAY
- USER-SERVICE
- MESSAGE-SERVICE

### 3. Test API Gateway

```bash
# Health check
curl http://54.217.247.163:8082/actuator/health

# Test user service via gateway
curl http://54.217.247.163:8082/user-service/actuator/health

# Test message service via gateway
curl http://54.217.247.163:8082/message-service/actuator/health
```

### 4. Test Frontend

Open browser: `http://54.154.129.84`

**Expected**: Angular application loads successfully

### 5. Test End-to-End Flow

1. Register new user via frontend
2. Login
3. Send message
4. Verify WebSocket connection
5. Check message delivery

---

## 🔧 Troubleshooting

### Problem: Service Not Registering with Eureka

**Solution**:
```bash
# Check service logs
docker-compose logs -f [service-name]

# Verify Eureka URL in environment
docker-compose exec [service] env | grep EUREKA

# Check network connectivity
docker-compose exec [service] ping 54.217.247.163

# Restart service
docker-compose restart [service]
```

### Problem: MySQL Connection Refused

**Solution**:
```bash
# Check MySQL is running
ssh ec2-user@3.147.141.101 "docker ps | grep mysql"

# Check firewall allows 35.153.96.103
# In Lightsail Console: Networking > Firewall > Check rules

# Test connection from services instance
ssh ec2-user@35.153.96.103
telnet 3.147.141.101 3306

# Check MySQL logs
ssh ec2-user@3.147.141.101
cd ~/chat-deployment/basic-chat-app/deployment-distributed/instance-3-mysql
docker-compose logs mysql
```

### Problem: Kafka Connection Failed

**Solution**:
```bash
# Check Kafka is running
ssh ec2-user@3.147.109.193 "docker ps | grep kafka"

# Verify advertised listeners
docker-compose exec kafka kafka-configs --bootstrap-server localhost:9092 --describe --entity-type brokers --all

# Test from message service
ssh ec2-user@35.153.96.103
telnet 3.147.109.193 9092

# Check Kafka logs
ssh ec2-user@3.147.109.193
docker-compose logs kafka
```

### Problem: Frontend API Calls Failing (CORS)

**Solution**:
```bash
# Check API Gateway CORS config
ssh ec2-user@54.217.247.163
docker-compose logs gateway | grep CORS

# Verify environment variable
docker-compose exec gateway env | grep CORS_ALLOWED_ORIGINS

# Should include: http://54.154.129.84

# Update if needed
# Edit docker-compose.yml
# Add: CORS_ALLOWED_ORIGINS=http://54.154.129.84
# Restart: docker-compose restart gateway
```

### Problem: WebSocket Connection Failed

**Solution**:
```bash
# Check message service WebSocket config
ssh ec2-user@35.153.96.103
docker-compose logs message-service | grep WebSocket

# Verify allowed origins
docker-compose exec message-service env | grep WEBSOCKET_ALLOWED_ORIGINS

# Test WebSocket from browser console:
# new WebSocket('ws://35.153.96.103:8083/ws')

# Check firewall allows 54.154.129.84 to 35.153.96.103:8083
```

---

## 📊 Monitoring

### Check Resource Usage

```bash
# Check each instance
ssh ec2-user@[INSTANCE_IP] "docker stats --no-stream"

# Expected memory usage:
# Instance 1: ~500MB (Eureka 256MB + Gateway 256MB)
# Instance 2: ~1GB (User 512MB + Message 512MB)
# Instance 3: ~400MB (MySQL)
# Instance 4: ~1GB (Kafka)
# Instance 5: ~100MB (Redis)
# Instance 6: ~50MB (Nginx)
```

### Check Logs

```bash
# View real-time logs
docker-compose logs -f [service-name]

# Last 100 lines
docker-compose logs --tail=100 [service-name]

# Search for errors
docker-compose logs [service-name] | grep -i error
```

### Check Kafka Topics

```bash
ssh ec2-user@3.147.109.193
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Expected topics:
# chat.message.sent
# chat.message.delivered
# chat.message.read
# chat.user.presence
```

### Check Redis Keys

```bash
ssh ec2-user@98.89.238.241
docker-compose exec redis redis-cli KEYS "presence:*"

# Example output:
# 1) "presence:user:1"
# 2) "presence:user:2"
```

---

## 🌐 DNS Migration Plan

### Current: Static IPs

All services use hardcoded IP addresses from `.env` file.

### Future: DNS Names

#### Step 1: Register Domain

Example: `yourchatapp.com`

#### Step 2: Create DNS Records

```
A record: eureka.yourchatapp.com    -> 54.217.247.163
A record: api.yourchatapp.com       -> 54.217.247.163
A record: services.yourchatapp.com  -> 35.153.96.103
A record: mysql.yourchatapp.com     -> 3.147.141.101
A record: kafka.yourchatapp.com     -> 3.147.109.193
A record: redis.yourchatapp.com     -> 98.89.238.241
A record: chat.yourchatapp.com      -> 54.154.129.84
```

#### Step 3: Update Environment Variables

Edit `shared-config/.env`:

```properties
# Replace IPs with DNS
EUREKA_URL=http://eureka.yourchatapp.com:8761/eureka/
API_GATEWAY_URL=http://api.yourchatapp.com:8082
MYSQL_HOST=mysql.yourchatapp.com
KAFKA_BOOTSTRAP_SERVERS=kafka.yourchatapp.com:9092
REDIS_HOST=redis.yourchatapp.com
FRONTEND_URL=https://chat.yourchatapp.com
```

#### Step 4: Enable HTTPS

See `instance-6-frontend/docker-compose.yml` for Certbot setup instructions.

---

## 📝 Deployment Checklist

Before going live:

- [ ] All static IPs attached to instances
- [ ] Firewall rules configured correctly
- [ ] MySQL database initialized
- [ ] Kafka topics created (auto-created by app)
- [ ] Eureka server accessible
- [ ] All services registered with Eureka
- [ ] API Gateway routing working
- [ ] Frontend connecting to API Gateway
- [ ] WebSocket connections working
- [ ] User registration working
- [ ] Message sending/receiving working
- [ ] Presence tracking working (online/offline)
- [ ] Monitoring setup (CloudWatch or alternative)
- [ ] Backup strategy implemented
- [ ] SSL certificates configured (for production)

---

## 🆘 Emergency Contacts

**Repository**: https://github.com/rakes9146/basic-chat-app  
**Docker Hub**: https://hub.docker.com/u/rakes9146  
**Documentation**: See `DOCKER-COMMANDS-REFERENCE.txt` for detailed commands

---

## 📚 Additional Resources

- [Single Instance Backup](../deployment-backup-single-instance/README.md)
- [Firewall Configuration](shared-config/FIREWALL-RULES.txt)
- [Environment Variables](shared-config/.env)
- [Docker Commands Reference](../DOCKER-COMMANDS-REFERENCE.txt)

---

**Deployment Date**: December 15, 2025  
**Version**: 1.0.0 (Distributed Architecture)
