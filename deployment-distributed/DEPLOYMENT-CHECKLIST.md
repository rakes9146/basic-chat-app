# 🎯 Pre-Deployment Checklist

## AWS Lightsail Setup

### ✅ Instance Creation
- [ ] Instance 1: 1GB RAM (Eureka + Gateway) - Created
- [ ] Instance 2: 2GB RAM (User + Message) - Created
- [ ] Instance 3: 1GB RAM (MySQL) - Created
- [ ] Instance 4: 2GB RAM (Kafka) - Created
- [ ] Instance 5: 512MB RAM (Redis) - Created
- [ ] Instance 6: 1GB RAM (Frontend) - Created

### ✅ Static IP Assignment
- [ ] 54.217.247.163 → Instance 1 (Eureka + Gateway)
- [ ] 35.153.96.103 → Instance 2 (User + Message)
- [ ] 3.147.141.101 → Instance 3 (MySQL)
- [ ] 3.147.109.193 → Instance 4 (Kafka)
- [ ] 98.89.238.241 → Instance 5 (Redis)
- [ ] 54.154.129.84 → Instance 6 (Frontend)

### ✅ Firewall Configuration
- [ ] Instance 1: Port 8761 (Eureka) - Optional
- [ ] Instance 1: Port 8082 (Gateway) - Public (0.0.0.0/0)
- [ ] Instance 2: Port 8081 (User) - Internal (54.217.247.163/32)
- [ ] Instance 2: Port 8083 (Message) - Internal (54.217.247.163/32 + 54.154.129.84/32)
- [ ] Instance 3: Port 3306 (MySQL) - Internal (35.153.96.103/32)
- [ ] Instance 4: Port 9092 (Kafka) - Internal (35.153.96.103/32)
- [ ] Instance 5: Port 6379 (Redis) - Internal (35.153.96.103/32)
- [ ] Instance 6: Port 80 (HTTP) - Public (0.0.0.0/0)
- [ ] Instance 6: Port 443 (HTTPS) - Public (0.0.0.0/0) - For future

---

## Local Machine Setup

### ✅ Prerequisites Installed
- [ ] Docker Desktop running
- [ ] Maven 3.9+ installed
- [ ] Node.js 20+ installed
- [ ] Git configured
- [ ] Docker Hub account logged in (`docker login`)

### ✅ Code Repository
- [ ] Repository cloned locally
- [ ] Latest code pulled from `main` branch
- [ ] All branches merged
- [ ] Working directory clean

---

## Build & Push Images

### ✅ Build Spring Boot Services
- [ ] Eureka Server: `mvn clean package -DskipTests`
- [ ] API Gateway: `mvn clean package -DskipTests`
- [ ] User Service: `mvn clean package -DskipTests`
- [ ] Message Service: `mvn clean package -DskipTests`

### ✅ Build Frontend
- [ ] `npm install` completed
- [ ] `npm run build --configuration production` successful
- [ ] `dist/frontend/browser` folder exists

### ✅ Build Docker Images
- [ ] `rakes9146/chat-eureka:distributed` built
- [ ] `rakes9146/chat-api-gateway:distributed` built
- [ ] `rakes9146/chat-user-service:distributed` built
- [ ] `rakes9146/chat-message-service:distributed` built
- [ ] `rakes9146/chat-frontend:distributed` built

### ✅ Push to Docker Hub
- [ ] `docker push rakes9146/chat-eureka:distributed`
- [ ] `docker push rakes9146/chat-api-gateway:distributed`
- [ ] `docker push rakes9146/chat-user-service:distributed`
- [ ] `docker push rakes9146/chat-message-service:distributed`
- [ ] `docker push rakes9146/chat-frontend:distributed`

---

## AWS Instance Preparation

### ✅ Install Docker on All Instances
- [ ] Instance 1: Docker installed and running
- [ ] Instance 2: Docker installed and running
- [ ] Instance 3: Docker installed and running
- [ ] Instance 4: Docker installed and running
- [ ] Instance 5: Docker installed and running
- [ ] Instance 6: Docker installed and running

### ✅ Install Docker Compose on All Instances
- [ ] Instance 1: Docker Compose installed
- [ ] Instance 2: Docker Compose installed
- [ ] Instance 3: Docker Compose installed
- [ ] Instance 4: Docker Compose installed
- [ ] Instance 5: Docker Compose installed
- [ ] Instance 6: Docker Compose installed

### ✅ Clone Repository on All Instances
- [ ] Instance 1: Repository cloned
- [ ] Instance 2: Repository cloned
- [ ] Instance 3: Repository cloned
- [ ] Instance 4: Repository cloned
- [ ] Instance 5: Repository cloned
- [ ] Instance 6: Repository cloned

---

## Deploy Services (In Order)

### ✅ Phase 1: Infrastructure (MySQL, Kafka, Redis)

#### MySQL (Instance 3: 3.147.141.101)
- [ ] Navigate to `deployment-distributed/instance-3-mysql`
- [ ] Run `docker-compose up -d`
- [ ] Verify: `docker-compose logs -f mysql`
- [ ] Test: `docker-compose exec mysql mysql -u chatuser -pchatpass123 -e "SHOW DATABASES;"`
- [ ] Database `chatdb` exists

#### Kafka (Instance 4: 3.147.109.193)
- [ ] Navigate to `deployment-distributed/instance-4-kafka`
- [ ] Run `docker-compose up -d`
- [ ] Verify: `docker-compose logs -f kafka`
- [ ] Test: `docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092`

#### Redis (Instance 5: 98.89.238.241)
- [ ] Navigate to `deployment-distributed/instance-5-redis`
- [ ] Run `docker-compose up -d`
- [ ] Verify: `docker-compose logs -f redis`
- [ ] Test: `docker-compose exec redis redis-cli ping`
- [ ] Response: `PONG`

### ✅ Phase 2: Service Discovery & Gateway

#### Eureka + Gateway (Instance 1: 54.217.247.163)
- [ ] Navigate to `deployment-distributed/instance-1-eureka-gateway`
- [ ] Remove `build:` sections from docker-compose.yml (keep `image:` only)
- [ ] Run `docker-compose pull`
- [ ] Run `docker-compose up -d`
- [ ] Verify Eureka: `docker-compose logs -f eureka`
- [ ] Test Eureka: `curl http://localhost:8761/actuator/health`
- [ ] Verify Gateway: `docker-compose logs -f gateway`
- [ ] Test Gateway: `curl http://localhost:8082/actuator/health`
- [ ] Access Eureka Dashboard: `http://54.217.247.163:8761`

### ✅ Phase 3: Business Services

#### User + Message Services (Instance 2: 35.153.96.103)
- [ ] Navigate to `deployment-distributed/instance-2-user-message-services`
- [ ] Remove `build:` sections from docker-compose.yml
- [ ] Run `docker-compose pull`
- [ ] Run `docker-compose up -d`
- [ ] Verify User Service: `docker-compose logs -f user-service`
- [ ] Test User Service: `curl http://localhost:8081/actuator/health`
- [ ] Verify Message Service: `docker-compose logs -f message-service`
- [ ] Test Message Service: `curl http://localhost:8083/actuator/health`
- [ ] Check Eureka registration: Services appear in dashboard

### ✅ Phase 4: Frontend

#### Frontend (Instance 6: 54.154.129.84)
- [ ] Navigate to `deployment-distributed/instance-6-frontend`
- [ ] Remove `build:` section from docker-compose.yml
- [ ] Run `docker-compose pull`
- [ ] Run `docker-compose up -d`
- [ ] Verify: `docker-compose logs -f frontend`
- [ ] Test: `curl http://localhost/`
- [ ] Access: `http://54.154.129.84`
- [ ] Frontend loads successfully

---

## End-to-End Testing

### ✅ Service Health Checks
- [ ] Eureka Dashboard shows all services: `http://54.217.247.163:8761`
- [ ] API Gateway health: `curl http://54.217.247.163:8082/actuator/health`
- [ ] User Service via Gateway: `curl http://54.217.247.163:8082/user-service/actuator/health`
- [ ] Message Service via Gateway: `curl http://54.217.247.163:8082/message-service/actuator/health`

### ✅ Frontend Testing
- [ ] Open: `http://54.154.129.84`
- [ ] Application loads without errors
- [ ] No console errors in browser DevTools

### ✅ User Registration Flow
- [ ] Navigate to registration page
- [ ] Fill registration form
- [ ] Click "Register"
- [ ] User created successfully
- [ ] Check MySQL: User record exists

### ✅ Login Flow
- [ ] Navigate to login page
- [ ] Enter credentials
- [ ] Click "Login"
- [ ] Redirect to chat page
- [ ] JWT token received

### ✅ Messaging Flow
- [ ] Send message from User A
- [ ] Message saved to MySQL
- [ ] Kafka event published to `chat.message.sent`
- [ ] User B receives message via WebSocket
- [ ] Message appears in User B's chat

### ✅ Presence Tracking
- [ ] User A logs in
- [ ] Redis key created: `presence:user:{userId} = online`
- [ ] User B sees "User A is online"
- [ ] User A logs out
- [ ] Redis key updated: `presence:user:{userId} = offline`
- [ ] User B sees "User A is offline"

### ✅ Read Receipts
- [ ] User B reads message
- [ ] Kafka event published to `chat.message.read`
- [ ] User A sees "Read" status

---

## Monitoring & Verification

### ✅ Resource Usage
- [ ] Instance 1: Memory < 900MB (1GB limit)
- [ ] Instance 2: Memory < 1.8GB (2GB limit)
- [ ] Instance 3: Memory < 900MB (1GB limit)
- [ ] Instance 4: Memory < 1.8GB (2GB limit)
- [ ] Instance 5: Memory < 450MB (512MB limit)
- [ ] Instance 6: Memory < 900MB (1GB limit)

### ✅ Logs Check
- [ ] No errors in Eureka logs
- [ ] No errors in Gateway logs
- [ ] No errors in User Service logs
- [ ] No errors in Message Service logs
- [ ] No errors in MySQL logs
- [ ] No errors in Kafka logs
- [ ] No errors in Redis logs
- [ ] No errors in Frontend logs

### ✅ Database Verification
- [ ] MySQL: `chatdb` database exists
- [ ] Tables created: `users`, `messages`, `message_status`
- [ ] Sample data visible
- [ ] Connections from User/Message services working

### ✅ Kafka Verification
- [ ] Topics exist: `chat.message.sent`, `chat.message.delivered`, `chat.message.read`, `chat.user.presence`
- [ ] Messages published successfully
- [ ] Consumer groups active
- [ ] No lag in consumption

### ✅ Redis Verification
- [ ] Connection successful
- [ ] Keys created: `presence:user:*`
- [ ] Values correct: `online`/`offline`
- [ ] TTL not set (data persists)

---

## Security Verification

### ✅ Firewall Rules
- [ ] MySQL accessible ONLY from 35.153.96.103
- [ ] Kafka accessible ONLY from 35.153.96.103
- [ ] Redis accessible ONLY from 35.153.96.103
- [ ] API Gateway accessible from public internet
- [ ] Frontend accessible from public internet
- [ ] User/Message services NOT accessible from public internet

### ✅ Credentials Security
- [ ] MySQL password changed from default
- [ ] No passwords in git history
- [ ] Environment variables not exposed
- [ ] SSH keys secured

---

## Documentation

### ✅ Documentation Complete
- [ ] README.md reviewed
- [ ] QUICK-START.md reviewed
- [ ] FOLDER-STRUCTURE.md reviewed
- [ ] FIREWALL-RULES.txt configured
- [ ] .env file with correct IPs

---

## Post-Deployment

### ✅ Backup Strategy
- [ ] MySQL backup script configured
- [ ] Kafka data backup plan
- [ ] Redis snapshot policy
- [ ] Volume backup schedule

### ✅ Monitoring Setup
- [ ] AWS CloudWatch enabled (optional)
- [ ] Log aggregation configured (optional)
- [ ] Alert rules defined (optional)

### ✅ DNS Migration Plan (Future)
- [ ] Domain registered
- [ ] DNS records planned
- [ ] SSL certificates planned

---

## ✅ Go-Live Approval

- [ ] All checklist items above completed
- [ ] End-to-end testing successful
- [ ] Performance acceptable
- [ ] Security verified
- [ ] Team notified
- [ ] Documentation shared

---

## 🎉 Deployment Complete!

**Date**: _________________  
**Deployed By**: _________________  
**Version**: 1.0.0 (Distributed Architecture)  

**Access URLs**:
- Frontend: http://54.154.129.84
- API Gateway: http://54.217.247.163:8082
- Eureka Dashboard: http://54.217.247.163:8761

---

## 📞 Support

**Documentation**: See `deployment-distributed/README.md`  
**Troubleshooting**: See `deployment-distributed/README.md#troubleshooting`  
**Repository**: https://github.com/rakes9146/basic-chat-app
