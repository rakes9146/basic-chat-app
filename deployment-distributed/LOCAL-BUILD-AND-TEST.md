# 🏠 Local Build & Test Guide

Complete guide to build Docker images and test locally before AWS deployment.

---

## 📋 Prerequisites

- Docker Desktop installed and running
- PowerShell terminal
- At least 4GB RAM available for Docker

---

## 🔨 Step 1: Build All Docker Images

Open PowerShell and navigate to project:

```powershell
cd "C:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app"
```

### Build Eureka Server

```powershell
cd deployment-distributed\instance-1-eureka-gateway
docker build -f Dockerfile.eureka -t rakes9146/chat-eureka:distributed ..\..\
```

**Expected Output:**
```
[+] Building 120.5s (14/14) FINISHED
=> [build 1/6] FROM docker.io/library/maven:3.9.6-eclipse-temurin-21
=> CACHED [build 2/6] WORKDIR /app
=> [build 3/6] COPY eurekaserver/pom.xml ./eurekaserver/
=> [build 4/6] RUN mvn -f eurekaserver/pom.xml dependency:go-offline
=> [build 5/6] COPY eurekaserver/src ./eurekaserver/src
=> [build 6/6] RUN mvn -f eurekaserver/pom.xml clean package -DskipTests
=> [runtime 1/3] WORKDIR /app
=> [runtime 2/3] COPY --from=build /app/eurekaserver/target/*.jar app.jar
=> exporting to image
=> => naming to docker.io/rakes9146/chat-eureka:distributed
```

### Build API Gateway

```powershell
docker build -f Dockerfile.gateway -t rakes9146/chat-api-gateway:distributed ..\..\
```

### Build User Service

```powershell
cd ..\instance-2-user-message-services
docker build -f Dockerfile.user -t rakes9146/chat-user-service:distributed ..\..\
```

### Build Message Service

```powershell
docker build -f Dockerfile.message -t rakes9146/chat-message-service:distributed ..\..\
```

### Build Frontend

```powershell
cd ..\instance-6-frontend
docker build -t rakes9146/chat-frontend:distributed ..\..\frontend
```

**If you need to build frontend Dockerfile**, it should be in `frontend/` folder:

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist/frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## ✅ Step 2: Verify Images Built

```powershell
docker images | Select-String "chat-"
```

**Expected Output:**
```
rakes9146/chat-eureka           distributed   abc123def456   2 minutes ago   350MB
rakes9146/chat-api-gateway      distributed   def456ghi789   3 minutes ago   345MB
rakes9146/chat-user-service     distributed   ghi789jkl012   5 minutes ago   360MB
rakes9146/chat-message-service  distributed   jkl012mno345   6 minutes ago   365MB
rakes9146/chat-frontend         distributed   mno345pqr678   8 minutes ago   45MB
```

---

## 🧪 Step 3: Test Locally (All Services Together)

### Use Local Testing Compose File

```powershell
cd "C:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\deployment-distributed"
docker-compose -f docker-compose-local.yml up -d
```

### Monitor Startup

```powershell
# Watch all logs
docker-compose -f docker-compose-local.yml logs -f

# Or watch specific service
docker-compose -f docker-compose-local.yml logs -f eureka-server
docker-compose -f docker-compose-local.yml logs -f user-service
docker-compose -f docker-compose-local.yml logs -f message-service
```

### Wait for Services to Start (90-120 seconds)

**Startup Order:**
1. MySQL (10s)
2. Kafka (30s)
3. Redis (5s)
4. Eureka Server (30s)
5. API Gateway (20s)
6. User Service (30s)
7. Message Service (30s)
8. Frontend (5s)

---

## 🔍 Step 4: Verify Local Deployment

### Check All Containers Running

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

**Expected Output:**
```
NAMES                  STATUS          PORTS
eureka-server         Up 2 minutes    0.0.0.0:8761->8761/tcp
api-gateway           Up 2 minutes    0.0.0.0:8082->8082/tcp
user-service          Up 90 seconds   0.0.0.0:8081->8081/tcp
message-service       Up 90 seconds   0.0.0.0:8083->8083/tcp
frontend              Up 2 minutes    0.0.0.0:80->80/tcp
mysql                 Up 3 minutes    0.0.0.0:3306->3306/tcp
kafka                 Up 3 minutes    0.0.0.0:9092->9092/tcp
redis                 Up 3 minutes    0.0.0.0:6379->6379/tcp
```

### Test Endpoints

```powershell
# Eureka Dashboard
Start-Process "http://localhost:8761"

# Check Gateway Health
curl http://localhost:8082/actuator/health

# Check User Service
curl http://localhost:8081/actuator/health

# Check Message Service
curl http://localhost:8083/actuator/health

# Open Frontend
Start-Process "http://localhost"
```

### Check Eureka Registry

Open http://localhost:8761 and verify:
- ✅ API-GATEWAY registered
- ✅ USER-SERVICE registered
- ✅ MESSAGE-SERVICE registered

### Test Application Flow

1. **Open Frontend**: http://localhost
2. **Register User**: Click "Register" → Enter details → Submit
3. **Login**: Enter credentials → Login
4. **Send Message**: Select user → Type message → Send
5. **Verify**: Check message appears in chat

---

## 📊 Step 5: Monitor Resources

```powershell
# Check CPU/Memory usage
docker stats --no-stream

# Check specific service logs
docker logs eureka-server
docker logs user-service --tail 100
docker logs message-service --tail 100

# Check database
docker exec -it mysql mysql -uroot -prootpassword chatapp -e "SHOW TABLES;"

# Check Kafka topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Check Redis
docker exec -it redis redis-cli ping
```

---

## 🛑 Step 6: Stop Local Testing

```powershell
# Stop all containers
docker-compose -f docker-compose-local.yml down

# Or stop but keep data
docker-compose -f docker-compose-local.yml stop

# Clean up everything (including volumes)
docker-compose -f docker-compose-local.yml down -v
```

---

## 🐛 Troubleshooting

### Issue 1: Service Won't Start

```powershell
# Check logs
docker-compose -f docker-compose-local.yml logs [service-name]

# Restart specific service
docker-compose -f docker-compose-local.yml restart [service-name]

# Remove and recreate
docker-compose -f docker-compose-local.yml up -d --force-recreate [service-name]
```

### Issue 2: Port Already in Use

```powershell
# Find process using port
netstat -ano | findstr ":8761"
netstat -ano | findstr ":8082"

# Stop the process
Stop-Process -Id [PID] -Force
```

### Issue 3: Service Can't Connect to MySQL

```powershell
# Check MySQL is running
docker ps | findstr mysql

# Test MySQL connection
docker exec -it mysql mysql -uroot -prootpassword -e "SELECT 1;"

# Check MySQL logs
docker logs mysql
```

### Issue 4: Eureka Shows Service as DOWN

Wait 30-60 seconds for registration. If still DOWN:

```powershell
# Restart the service
docker-compose -f docker-compose-local.yml restart user-service

# Check service can reach Eureka
docker exec -it user-service ping eureka-server
```

### Issue 5: Build Fails

```powershell
# Clean Docker build cache
docker builder prune -a

# Rebuild without cache
docker build --no-cache -f Dockerfile.eureka -t rakes9146/chat-eureka:distributed ..\..\
```

---

## 📦 Step 7: Push Images to Docker Hub (For AWS Deployment)

### Login to Docker Hub

```powershell
docker login
# Enter username: rakes9146
# Enter password: [your-password]
```

### Push All Images

```powershell
docker push rakes9146/chat-eureka:distributed
docker push rakes9146/chat-api-gateway:distributed
docker push rakes9146/chat-user-service:distributed
docker push rakes9146/chat-message-service:distributed
docker push rakes9146/chat-frontend:distributed
```

**Expected Output (per image):**
```
The push refers to repository [docker.io/rakes9146/chat-eureka]
a1b2c3d4e5f6: Pushed
f6e5d4c3b2a1: Pushed
distributed: digest: sha256:abc123... size: 2841
```

### Verify on Docker Hub

Visit: https://hub.docker.com/u/rakes9146

Check all 5 images are present:
- ✅ chat-eureka:distributed
- ✅ chat-api-gateway:distributed
- ✅ chat-user-service:distributed
- ✅ chat-message-service:distributed
- ✅ chat-frontend:distributed

---

## ✅ Complete Build Script (All-in-One)

Save as `build-all.ps1`:

```powershell
# Build All Docker Images
$ErrorActionPreference = "Stop"

Write-Host "🔨 Building Docker Images..." -ForegroundColor Green
cd "C:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app"

Write-Host "`n📦 Building Eureka Server..." -ForegroundColor Yellow
cd deployment-distributed\instance-1-eureka-gateway
docker build -f Dockerfile.eureka -t rakes9146/chat-eureka:distributed ..\..\

Write-Host "`n📦 Building API Gateway..." -ForegroundColor Yellow
docker build -f Dockerfile.gateway -t rakes9146/chat-api-gateway:distributed ..\..\

Write-Host "`n📦 Building User Service..." -ForegroundColor Yellow
cd ..\instance-2-user-message-services
docker build -f Dockerfile.user -t rakes9146/chat-user-service:distributed ..\..\

Write-Host "`n📦 Building Message Service..." -ForegroundColor Yellow
docker build -f Dockerfile.message -t rakes9146/chat-message-service:distributed ..\..\

Write-Host "`n📦 Building Frontend..." -ForegroundColor Yellow
cd ..\instance-6-frontend
docker build -t rakes9146/chat-frontend:distributed ..\..\frontend

Write-Host "`n✅ All images built successfully!" -ForegroundColor Green
docker images | Select-String "chat-"

Write-Host "`n📤 Ready to push to Docker Hub? Run:" -ForegroundColor Cyan
Write-Host "docker login" -ForegroundColor White
Write-Host "docker push rakes9146/chat-eureka:distributed" -ForegroundColor White
Write-Host "docker push rakes9146/chat-api-gateway:distributed" -ForegroundColor White
Write-Host "docker push rakes9146/chat-user-service:distributed" -ForegroundColor White
Write-Host "docker push rakes9146/chat-message-service:distributed" -ForegroundColor White
Write-Host "docker push rakes9146/chat-frontend:distributed" -ForegroundColor White
```

Run it:
```powershell
.\build-all.ps1
```

---

## ✅ Complete Push Script (All-in-One)

Save as `push-all.ps1`:

```powershell
# Push All Images to Docker Hub
$ErrorActionPreference = "Stop"

Write-Host "📤 Pushing images to Docker Hub..." -ForegroundColor Green

docker push rakes9146/chat-eureka:distributed
docker push rakes9146/chat-api-gateway:distributed
docker push rakes9146/chat-user-service:distributed
docker push rakes9146/chat-message-service:distributed
docker push rakes9146/chat-frontend:distributed

Write-Host "`n✅ All images pushed successfully!" -ForegroundColor Green
Write-Host "🌐 Check: https://hub.docker.com/u/rakes9146" -ForegroundColor Cyan
```

Run it:
```powershell
docker login
.\push-all.ps1
```

---

## 🎯 Quick Reference

| Command | Purpose |
|---------|---------|
| `docker images` | List built images |
| `docker-compose -f docker-compose-local.yml up -d` | Start all services locally |
| `docker-compose -f docker-compose-local.yml logs -f` | Watch logs |
| `docker-compose -f docker-compose-local.yml ps` | Check status |
| `docker-compose -f docker-compose-local.yml down` | Stop everything |
| `docker stats` | Monitor resources |
| `docker push [image]` | Push to Docker Hub |

---

**Next Step**: After local testing succeeds and images are pushed, proceed to AWS deployment! 🚀
