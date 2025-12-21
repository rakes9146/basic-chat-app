# 📁 Distributed Deployment - Folder Structure

## Overview

This document shows the complete folder structure for the distributed multi-instance deployment architecture.

---

## 🗂️ Complete Structure

```
basic-chat-app/
│
├── deployment-backup-single-instance/          # Old single-instance setup (backup)
│   ├── README.md                               # Explanation of backup
│   ├── docker-compose-original.yml             # Original compose file
│   └── .env-original                           # Original environment
│
├── deployment-distributed/                     # New distributed architecture
│   │
│   ├── instance-1-eureka-gateway/              # Instance 1: 54.217.247.163 (1GB)
│   │   ├── Dockerfile.eureka                   # Eureka Server Dockerfile
│   │   ├── Dockerfile.gateway                  # API Gateway Dockerfile
│   │   └── docker-compose.yml                  # Compose for both services
│   │
│   ├── instance-2-user-message-services/       # Instance 2: 35.153.96.103 (2GB)
│   │   ├── Dockerfile.user                     # User Service Dockerfile
│   │   ├── Dockerfile.message                  # Message Service Dockerfile
│   │   └── docker-compose.yml                  # Compose for both services
│   │
│   ├── instance-3-mysql/                       # Instance 3: 3.147.141.101 (1GB)
│   │   └── docker-compose.yml                  # MySQL configuration
│   │
│   ├── instance-4-kafka/                       # Instance 4: 3.147.109.193 (2GB)
│   │   └── docker-compose.yml                  # Kafka configuration
│   │
│   ├── instance-5-redis/                       # Instance 5: 98.89.238.241 (512MB)
│   │   └── docker-compose.yml                  # Redis configuration
│   │
│   ├── instance-6-frontend/                    # Instance 6: 54.154.129.84 (1GB)
│   │   └── docker-compose.yml                  # Frontend (Nginx) configuration
│   │
│   ├── shared-config/                          # Shared configuration files
│   │   ├── .env                                # Environment variables (all IPs)
│   │   └── FIREWALL-RULES.txt                  # AWS Lightsail firewall config
│   │
│   ├── README.md                               # Complete deployment guide
│   ├── QUICK-START.md                          # 30-minute quick start
│   └── FOLDER-STRUCTURE.md                     # This file
│
├── eurekaserver/                               # Eureka Server source code
├── apigateway/                                 # API Gateway source code
├── user-service/                               # User Service source code
├── message-service/                            # Message Service source code
├── frontend/                                   # Angular frontend source code
│
├── docker-compose.yml                          # (Original - for local development)
├── .env                                        # (Original - for local development)
├── DOCKER-COMMANDS-REFERENCE.txt               # Single-instance deployment reference
└── README.md                                   # Project README
```

---

## 📋 File Descriptions

### Instance 1: Eureka + API Gateway (1GB - 54.217.247.163)

**Purpose**: Service discovery and API routing

| File | Description |
|------|-------------|
| `Dockerfile.eureka` | Builds Eureka Server with 256MB heap |
| `Dockerfile.gateway` | Builds API Gateway with 256MB heap |
| `docker-compose.yml` | Orchestrates both services, connects to Eureka |

**Key Environment Variables**:
- `EUREKA_INSTANCE_HOSTNAME=54.217.247.163`
- `USER_SERVICE_URL=http://35.153.96.103:8081`
- `MESSAGE_SERVICE_URL=http://35.153.96.103:8083`

---

### Instance 2: User + Message Services (2GB - 35.153.96.103)

**Purpose**: Core business logic services

| File | Description |
|------|-------------|
| `Dockerfile.user` | Builds User Service with 512MB heap |
| `Dockerfile.message` | Builds Message Service with 512MB heap |
| `docker-compose.yml` | Orchestrates both services, connects to MySQL, Kafka, Redis, Eureka |

**Key Environment Variables**:
- `SPRING_DATASOURCE_URL=jdbc:mysql://3.147.141.101:3306/chatdb`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS=3.147.109.193:9092`
- `SPRING_REDIS_HOST=98.89.238.241`
- `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://54.217.247.163:8761/eureka/`

---

### Instance 3: MySQL (1GB - 3.147.141.101)

**Purpose**: Relational database

| File | Description |
|------|-------------|
| `docker-compose.yml` | MySQL 8.0 with optimized settings for 1GB RAM |

**Configuration**:
- Database: `chatdb`
- User: `chatuser`
- Password: `chatpass123`
- Buffer Pool: 256MB
- Max Connections: 50

---

### Instance 4: Kafka (2GB - 3.147.109.193)

**Purpose**: Message broker for real-time events

| File | Description |
|------|-------------|
| `docker-compose.yml` | Kafka 3.7.0 in KRaft mode (no ZooKeeper) |

**Topics** (auto-created):
- `chat.message.sent`
- `chat.message.delivered`
- `chat.message.read`
- `chat.user.presence`

---

### Instance 5: Redis (512MB - 98.89.238.241)

**Purpose**: Cache for user presence tracking

| File | Description |
|------|-------------|
| `docker-compose.yml` | Redis 7-alpine with 256MB memory limit |

**Usage**:
- Keys: `presence:user:{userId}`
- Values: `"online"` or `"offline"`

---

### Instance 6: Frontend (1GB - 54.154.129.84)

**Purpose**: Angular application served via Nginx

| File | Description |
|------|-------------|
| `docker-compose.yml` | Nginx serving production Angular build |

**Key Environment Variables**:
- `API_GATEWAY_URL=http://54.217.247.163:8082`
- `WEBSOCKET_URL=ws://35.153.96.103:8083/ws`

---

### Shared Configuration

**Purpose**: Common configuration across all instances

| File | Description |
|------|-------------|
| `.env` | All static IPs, credentials, URLs |
| `FIREWALL-RULES.txt` | Security rules for each instance |

---

## 🎯 Deployment Order

**Must deploy in this sequence**:

1. **Infrastructure Layer**: MySQL → Kafka → Redis
2. **Service Discovery**: Eureka
3. **API Layer**: API Gateway
4. **Business Logic**: User Service → Message Service
5. **Presentation Layer**: Frontend

---

## 🔐 Security Configuration

### Public Access (0.0.0.0/0)
- ✅ Frontend (80, 443)
- ✅ API Gateway (8082)
- ⚠️ Eureka (8761) - Optional, for monitoring

### Internal Access (35.153.96.103/32 only)
- 🔒 MySQL (3306)
- 🔒 Kafka (9092)
- 🔒 Redis (6379)
- 🔒 User Service (8081)
- 🔒 Message Service (8083)

See `shared-config/FIREWALL-RULES.txt` for detailed configuration.

---

## 📊 Resource Allocation

| Instance | RAM | Services | Expected Memory Usage |
|----------|-----|----------|----------------------|
| Instance 1 | 1GB | Eureka (256MB) + Gateway (256MB) | ~500MB |
| Instance 2 | 2GB | User (512MB) + Message (512MB) | ~1GB |
| Instance 3 | 1GB | MySQL | ~400MB |
| Instance 4 | 2GB | Kafka | ~1GB |
| Instance 5 | 512MB | Redis | ~100MB |
| Instance 6 | 1GB | Frontend (Nginx) | ~50MB |

**Total**: 8.5GB RAM across 6 instances

---

## 🚀 Quick Commands

### Build All Images (Local)
```bash
cd deployment-distributed/instance-1-eureka-gateway
docker build -f Dockerfile.eureka -t rakes9146/chat-eureka:distributed ../../
docker build -f Dockerfile.gateway -t rakes9146/chat-api-gateway:distributed ../../

cd ../instance-2-user-message-services
docker build -f Dockerfile.user -t rakes9146/chat-user-service:distributed ../../
docker build -f Dockerfile.message -t rakes9146/chat-message-service:distributed ../../

cd ../instance-6-frontend
docker build -t rakes9146/chat-frontend:distributed ../../frontend
```

### Push All Images
```bash
docker push rakes9146/chat-eureka:distributed
docker push rakes9146/chat-api-gateway:distributed
docker push rakes9146/chat-user-service:distributed
docker push rakes9146/chat-message-service:distributed
docker push rakes9146/chat-frontend:distributed
```

### Deploy on AWS (Each Instance)
```bash
# General pattern for each instance
ssh ec2-user@[INSTANCE_IP]
git clone https://github.com/rakes9146/basic-chat-app.git
cd basic-chat-app/deployment-distributed/instance-[N]-[NAME]
docker-compose up -d
```

---

## 📚 Documentation Navigation

| Document | Purpose |
|----------|---------|
| [README.md](README.md) | Complete deployment guide (detailed) |
| [QUICK-START.md](QUICK-START.md) | 30-minute quick deployment |
| [FOLDER-STRUCTURE.md](FOLDER-STRUCTURE.md) | This file - structure overview |
| [shared-config/.env](shared-config/.env) | All environment variables |
| [shared-config/FIREWALL-RULES.txt](shared-config/FIREWALL-RULES.txt) | Security configuration |

---

## 🔄 Migration from Single to Distributed

**Old Architecture** (1 instance, 2GB RAM):
```
deployment-backup-single-instance/
├── docker-compose-original.yml    # All services in one file
└── .env-original                   # Single instance configuration
```

**New Architecture** (6 instances, 8.5GB total):
```
deployment-distributed/
├── instance-1-eureka-gateway/     # Separate deployment per instance
├── instance-2-user-message-services/
├── instance-3-mysql/
├── instance-4-kafka/
├── instance-5-redis/
└── instance-6-frontend/
```

**Benefits**:
- ✅ Load distributed across instances
- ✅ Services can scale independently
- ✅ Better isolation and security
- ✅ Easier to debug individual services
- ✅ No single point of failure

---

## 🌐 Future Enhancements

### DNS Migration
Replace static IPs in `shared-config/.env` with DNS names:
- `eureka.yourdomain.com`
- `api.yourdomain.com`
- `mysql.yourdomain.com`
- `kafka.yourdomain.com`
- `redis.yourdomain.com`
- `chat.yourdomain.com`

### HTTPS Setup
Configure SSL certificates on:
- Frontend (80 → 443)
- API Gateway (8082 → 443)

See `instance-6-frontend/docker-compose.yml` for Certbot configuration.

---

**Last Updated**: December 15, 2025  
**Version**: 1.0.0
