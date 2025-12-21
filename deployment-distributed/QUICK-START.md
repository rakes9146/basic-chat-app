# Quick Start Guide - Distributed Deployment

## 🚀 TL;DR - Deploy in 30 Minutes

### Prerequisites
- 6 AWS Lightsail instances with Docker & Docker Compose installed
- Static IPs attached
- Firewall rules configured (see FIREWALL-RULES.txt)

---

## Step-by-Step Deployment

### 1️⃣ Build Images Locally (10 mins)

```bash
cd basic-chat-app

# Build all Spring Boot services
cd eurekaserver && mvn clean package -DskipTests && cd ..
cd apigateway && mvn clean package -DskipTests && cd ..
cd user-service && mvn clean package -DskipTests && cd ..
cd message-service && mvn clean package -DskipTests && cd ..

# Build frontend
cd frontend
npm install
npm run build --configuration production
cd ..

# Build Docker images
cd deployment-distributed/instance-1-eureka-gateway
docker build -f Dockerfile.eureka -t rakes9146/chat-eureka:distributed ../../
docker build -f Dockerfile.gateway -t rakes9146/chat-api-gateway:distributed ../../

cd ../instance-2-user-message-services
docker build -f Dockerfile.user -t rakes9146/chat-user-service:distributed ../../
docker build -f Dockerfile.message -t rakes9146/chat-message-service:distributed ../../

cd ../instance-6-frontend
docker build -t rakes9146/chat-frontend:distributed ../../frontend

# Push all images
docker push rakes9146/chat-eureka:distributed
docker push rakes9146/chat-api-gateway:distributed
docker push rakes9146/chat-user-service:distributed
docker push rakes9146/chat-message-service:distributed
docker push rakes9146/chat-frontend:distributed
```

### 2️⃣ Deploy Infrastructure (MySQL, Kafka, Redis) - 5 mins

```bash
# MySQL (3.147.141.101)
ssh ec2-user@3.147.141.101
git clone https://github.com/rakes9146/basic-chat-app.git
cd basic-chat-app/deployment-distributed/instance-3-mysql
docker-compose up -d
exit

# Kafka (3.147.109.193)
ssh ec2-user@3.147.109.193
cd basic-chat-app/deployment-distributed/instance-4-kafka
docker-compose up -d
exit

# Redis (98.89.238.241)
ssh ec2-user@98.89.238.241
cd basic-chat-app/deployment-distributed/instance-5-redis
docker-compose up -d
exit
```

### 3️⃣ Deploy Core Services (Eureka, Gateway, Services) - 5 mins

```bash
# Eureka + Gateway (54.217.247.163)
ssh ec2-user@54.217.247.163
cd basic-chat-app/deployment-distributed/instance-1-eureka-gateway

# Remove build sections from docker-compose.yml (keep only image: references)
docker-compose up -d
exit

# User + Message Services (35.153.96.103)
ssh ec2-user@35.153.96.103
cd basic-chat-app/deployment-distributed/instance-2-user-message-services

# Remove build sections from docker-compose.yml
docker-compose up -d
exit
```

### 4️⃣ Deploy Frontend - 2 mins

```bash
# Frontend (54.154.129.84)
ssh ec2-user@54.154.129.84
cd basic-chat-app/deployment-distributed/instance-6-frontend

# Remove build section from docker-compose.yml
docker-compose up -d
exit
```

---

## ✅ Verification (3 mins)

### Check Eureka Dashboard
```
http://54.217.247.163:8761
```
Should show: API-GATEWAY, USER-SERVICE, MESSAGE-SERVICE

### Test API Gateway
```bash
curl http://54.217.247.163:8082/actuator/health
```

### Test Frontend
```
http://54.154.129.84
```
Should load Angular application

---

## 🔧 Quick Troubleshooting

### Service Not Starting?
```bash
# Check logs
docker-compose logs -f [service-name]

# Restart service
docker-compose restart [service-name]
```

### Can't Connect to MySQL/Kafka/Redis?
```bash
# Check firewall rules allow 35.153.96.103
# Test connection
telnet [IP] [PORT]
```

### Service Not Registering with Eureka?
```bash
# Check Eureka URL
docker-compose exec [service] env | grep EUREKA

# Restart service
docker-compose restart [service]
```

---

## 📝 Important Files

- **Full Guide**: [README.md](README.md)
- **Firewall Rules**: [shared-config/FIREWALL-RULES.txt](shared-config/FIREWALL-RULES.txt)
- **Environment Variables**: [shared-config/.env](shared-config/.env)

---

## 🎯 Instance Overview

| Instance | IP | Services | RAM | Command |
|----------|-----|----------|-----|---------|
| 1 | 54.217.247.163 | Eureka + Gateway | 1GB | `ssh ec2-user@54.217.247.163` |
| 2 | 35.153.96.103 | User + Message | 2GB | `ssh ec2-user@35.153.96.103` |
| 3 | 3.147.141.101 | MySQL | 1GB | `ssh ec2-user@3.147.141.101` |
| 4 | 3.147.109.193 | Kafka | 2GB | `ssh ec2-user@3.147.109.193` |
| 5 | 98.89.238.241 | Redis | 512MB | `ssh ec2-user@98.89.238.241` |
| 6 | 54.154.129.84 | Frontend | 1GB | `ssh ec2-user@54.154.129.84` |

---

## 🚨 Critical Security

**NEVER expose to public (0.0.0.0/0)**:
- ❌ MySQL (3306)
- ❌ Kafka (9092)
- ❌ Redis (6379)

**Only expose to public**:
- ✅ Frontend (80, 443)
- ✅ API Gateway (8082)
- ✅ Eureka (8761) - Optional

---

Need detailed instructions? See [README.md](README.md)
