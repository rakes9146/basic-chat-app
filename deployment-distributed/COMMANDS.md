# 🚀 Quick Commands Reference

## Build Everything Locally

```powershell
# Navigate to project root
cd "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app"

# Build all Spring Boot services
cd eurekaserver && mvn clean package -DskipTests && cd ..
cd apigateway && mvn clean package -DskipTests && cd ..
cd user-service && mvn clean package -DskipTests && cd ..
cd message-service && mvn clean package -DskipTests && cd ..

# Build Angular frontend
cd frontend
npm install
npm run build --configuration production
cd ..
```

---

## Build Docker Images

```powershell
# Navigate to deployment folder
cd deployment-distributed

# Build Eureka & Gateway
cd instance-1-eureka-gateway
docker build -f Dockerfile.eureka -t rakes9146/chat-eureka:distributed ../../
docker build -f Dockerfile.gateway -t rakes9146/chat-api-gateway:distributed ../../

# Build User & Message Services
cd ../instance-2-user-message-services
docker build -f Dockerfile.user -t rakes9146/chat-user-service:distributed ../../
docker build -f Dockerfile.message -t rakes9146/chat-message-service:distributed ../../

# Build Frontend
cd ../instance-6-frontend
docker build -t rakes9146/chat-frontend:distributed ../../frontend

cd ..
```

---

## Test Locally

```powershell
# Start all services
docker-compose -f docker-compose-local.yml up -d

# Watch logs
docker-compose -f docker-compose-local.yml logs -f

# Check status
docker-compose -f docker-compose-local.yml ps

# Stop all
docker-compose -f docker-compose-local.yml down

# Stop and clean
docker-compose -f docker-compose-local.yml down -v
```

---

## Verify Local

```powershell
# Eureka Dashboard
Start-Process "http://localhost:8761"

# API Gateway Health
curl http://localhost:8082/actuator/health

# Frontend
Start-Process "http://localhost"
```

---

## Push to Docker Hub

```powershell
# Login
docker login

# Push all images
docker push rakes9146/chat-eureka:distributed
docker push rakes9146/chat-api-gateway:distributed
docker push rakes9146/chat-user-service:distributed
docker push rakes9146/chat-message-service:distributed
docker push rakes9146/chat-frontend:distributed
```

---

## Deploy to AWS (Each Instance)

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

# Eureka + Gateway (54.217.247.163)
ssh ec2-user@54.217.247.163
cd basic-chat-app/deployment-distributed/instance-1-eureka-gateway
# Remove build: sections from docker-compose.yml
docker-compose up -d
exit

# User + Message (35.153.96.103)
ssh ec2-user@35.153.96.103
cd basic-chat-app/deployment-distributed/instance-2-user-message-services
# Remove build: sections from docker-compose.yml
docker-compose up -d
exit

# Frontend (54.154.129.84)
ssh ec2-user@54.154.129.84
cd basic-chat-app/deployment-distributed/instance-6-frontend
# Remove build: section from docker-compose.yml
docker-compose up -d
exit
```

---

## Verify AWS Deployment

```
Eureka:     http://54.217.247.163:8761
Gateway:    http://54.217.247.163:8082/actuator/health
Frontend:   http://54.154.129.84
```

---

## Check Logs (AWS)

```bash
# On any instance
docker-compose logs -f [service-name]
docker-compose ps
docker stats --no-stream
```

---

## Complete Guide

See [LOCAL-TESTING.md](LOCAL-TESTING.md) for detailed local testing  
See [QUICK-START.md](QUICK-START.md) for detailed AWS deployment  
See [INDEX.md](INDEX.md) for all documentation
