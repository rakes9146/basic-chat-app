# 🚀 AWS Deployment - Docker Hub Images Only (No Source Code)

## ✅ Approach: Pull Pre-Built Images from Docker Hub

This approach:
- ✅ No source code needed on AWS instances
- ✅ Just copy docker-compose files + .env
- ✅ Pull images from Docker Hub
- ✅ Execute docker-compose

---

## 📦 Step 1: Build & Push Images (From Your Local Machine)

```powershell
# Navigate to project
cd "C:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app"

# Build all images
cd deployment-distributed\instance-1-eureka-gateway
docker build -f Dockerfile.eureka -t rakes9146/chat-eureka:distributed ..\..\
docker build -f Dockerfile.gateway -t rakes9146/chat-api-gateway:distributed ..\..\

cd ..\instance-2-user-message-services
docker build -f Dockerfile.user -t rakes9146/chat-user-service:distributed ..\..\
docker build -f Dockerfile.message -t rakes9146/chat-message-service:distributed ..\..\

cd ..\instance-6-frontend
docker build -t rakes9146/chat-frontend:distributed ..\..\frontend

# Push to Docker Hub
docker login
docker push rakes9146/chat-eureka:distributed
docker push rakes9146/chat-api-gateway:distributed
docker push rakes9146/chat-user-service:distributed
docker push rakes9146/chat-message-service:distributed
docker push rakes9146/chat-frontend:distributed
```

---

## 📂 Step 2: Create Deployment Package

Create a folder with only the files needed for deployment (no source code):

```powershell
# Create deployment package folder
cd "C:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app"
New-Item -ItemType Directory -Path "aws-deployment-package" -Force

# Copy only deployment files
Copy-Item -Recurse "deployment-distributed\instance-1-eureka-gateway" "aws-deployment-package\"
Copy-Item -Recurse "deployment-distributed\instance-2-user-message-services" "aws-deployment-package\"
Copy-Item -Recurse "deployment-distributed\instance-3-mysql" "aws-deployment-package\"
Copy-Item -Recurse "deployment-distributed\instance-4-kafka" "aws-deployment-package\"
Copy-Item -Recurse "deployment-distributed\instance-5-redis" "aws-deployment-package\"
Copy-Item -Recurse "deployment-distributed\instance-6-frontend" "aws-deployment-package\"
Copy-Item "deployment-distributed\shared-config\.env" "aws-deployment-package\"

# Zip it
Compress-Archive -Path "aws-deployment-package\*" -DestinationPath "aws-deployment.zip" -Force
```

Now you have `aws-deployment.zip` with only:
- docker-compose.yml files (for each instance)
- .env file
- No source code!

---

## 🌐 Step 3: Upload to AWS Instances

### Option 1: Upload via SCP

```powershell
# Upload zip to Instance 1 (Eureka + Gateway)
scp -i your-key.pem aws-deployment.zip ec2-user@54.217.247.163:~/

# Repeat for other instances
scp -i your-key.pem aws-deployment.zip ec2-user@35.153.96.103:~/
scp -i your-key.pem aws-deployment.zip ec2-user@3.147.141.101:~/
scp -i your-key.pem aws-deployment.zip ec2-user@3.147.109.193:~/
scp -i your-key.pem aws-deployment.zip ec2-user@98.89.238.241:~/
scp -i your-key.pem aws-deployment.zip ec2-user@54.154.129.84:~/
```

### Option 2: Create Simple Transfer Script

Save this as `upload-to-aws.ps1`:

```powershell
$instances = @{
    "54.217.247.163" = "Eureka+Gateway"
    "35.153.96.103" = "Services"
    "3.147.141.101" = "MySQL"
    "3.147.109.193" = "Kafka"
    "98.89.238.241" = "Redis"
    "54.154.129.84" = "Frontend"
}

foreach ($ip in $instances.Keys) {
    Write-Host "Uploading to $($instances[$ip]) ($ip)..."
    scp -i your-key.pem aws-deployment.zip ec2-user@${ip}:~/
}
```

---

## ⚙️ Step 4: Setup AWS Instances (Do Once on Each)

```bash
# SSH into instance
ssh -i your-key.pem ec2-user@[INSTANCE_IP]

# Install Docker
sudo dnf update -y
sudo dnf install docker unzip -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify
docker --version
docker-compose --version

# Unzip deployment files
unzip aws-deployment.zip -d deployment
cd deployment

# Log out and back in
exit
```

---

## 🎯 Step 5: Deploy Services (In Order)

### Instance 3: MySQL (3.147.141.101)

```bash
ssh ec2-user@3.147.141.101
cd deployment/instance-3-mysql

# Pull and start
docker-compose pull
docker-compose up -d

# Verify
docker-compose logs -f mysql
docker-compose ps
```

### Instance 4: Kafka (3.147.109.193)

```bash
ssh ec2-user@3.147.109.193
cd deployment/instance-4-kafka

# Pull and start
docker-compose pull
docker-compose up -d

# Wait 30 seconds, then verify
docker-compose logs -f kafka
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Instance 5: Redis (98.89.238.241)

```bash
ssh ec2-user@98.89.238.241
cd deployment/instance-5-redis

# Pull and start
docker-compose pull
docker-compose up -d

# Verify
docker-compose logs -f redis
docker-compose exec redis redis-cli ping
```

### Instance 1: Eureka + Gateway (54.217.247.163)

```bash
ssh ec2-user@54.217.247.163
cd deployment/instance-1-eureka-gateway

# Pull images from Docker Hub
docker-compose pull

# Start services
docker-compose up -d

# Verify
docker-compose logs -f
docker ps
curl http://localhost:8761/actuator/health
curl http://localhost:8082/actuator/health
```

**Check**: http://54.217.247.163:8761 (Eureka Dashboard)

### Instance 2: User + Message Services (35.153.96.103)

```bash
ssh ec2-user@35.153.96.103
cd deployment/instance-2-user-message-services

# Pull images
docker-compose pull

# Start services
docker-compose up -d

# Verify
docker-compose logs -f user-service
docker-compose logs -f message-service
docker ps
```

**Check**: Services should register in Eureka

### Instance 6: Frontend (54.154.129.84)

```bash
ssh ec2-user@54.154.129.84
cd deployment/instance-6-frontend

# Pull image
docker-compose pull

# Start frontend
docker-compose up -d

# Verify
docker-compose logs -f frontend
curl http://localhost/
```

**Access**: http://54.154.129.84

---

## ✅ Quick Deployment Commands (Per Instance)

### All Instances Follow Same Pattern:

```bash
# 1. SSH into instance
ssh ec2-user@[INSTANCE_IP]

# 2. Navigate to deployment folder
cd deployment/instance-[N]-[NAME]

# 3. Pull images from Docker Hub
docker-compose pull

# 4. Start services
docker-compose up -d

# 5. Check logs
docker-compose logs -f

# 6. Check status
docker-compose ps
docker stats --no-stream
```

---

## 📋 What Each Instance Needs

| Instance | Files Needed | Images Needed |
|----------|-------------|---------------|
| MySQL | docker-compose.yml | mysql:8.0 |
| Kafka | docker-compose.yml | apache/kafka:3.7.0 |
| Redis | docker-compose.yml | redis:7-alpine |
| Eureka+Gateway | docker-compose.yml | rakes9146/chat-eureka:distributed<br>rakes9146/chat-api-gateway:distributed |
| Services | docker-compose.yml | rakes9146/chat-user-service:distributed<br>rakes9146/chat-message-service:distributed |
| Frontend | docker-compose.yml | rakes9146/chat-frontend:distributed |

**No source code needed on any instance!** ✅

---

## 🔍 Verification Checklist

```bash
# On each instance, check:
docker ps                    # All containers running
docker-compose ps            # Service status
docker-compose logs [service] # Check for errors
docker stats --no-stream     # Resource usage
```

### Test Application Flow:
1. ✅ Eureka Dashboard: http://54.217.247.163:8761
2. ✅ API Gateway: http://54.217.247.163:8082/actuator/health
3. ✅ Frontend: http://54.154.129.84
4. ✅ Register user → Login → Send message

---

## 🔄 Update Deployment (When You Change Code)

```powershell
# 1. Build new images locally
docker build -f Dockerfile.user -t rakes9146/chat-user-service:distributed ../../

# 2. Push to Docker Hub
docker push rakes9146/chat-user-service:distributed

# 3. On AWS instance, pull and restart
ssh ec2-user@35.153.96.103
cd deployment/instance-2-user-message-services
docker-compose pull user-service
docker-compose up -d user-service
docker-compose logs -f user-service
```

---

## 📊 One-Line Deploy Commands (After Setup)

```bash
# MySQL
ssh ec2-user@3.147.141.101 'cd deployment/instance-3-mysql && docker-compose pull && docker-compose up -d'

# Kafka
ssh ec2-user@3.147.109.193 'cd deployment/instance-4-kafka && docker-compose pull && docker-compose up -d'

# Redis
ssh ec2-user@98.89.238.241 'cd deployment/instance-5-redis && docker-compose pull && docker-compose up -d'

# Eureka + Gateway
ssh ec2-user@54.217.247.163 'cd deployment/instance-1-eureka-gateway && docker-compose pull && docker-compose up -d'

# Services
ssh ec2-user@35.153.96.103 'cd deployment/instance-2-user-message-services && docker-compose pull && docker-compose up -d'

# Frontend
ssh ec2-user@54.154.129.84 'cd deployment/instance-6-frontend && docker-compose pull && docker-compose up -d'
```

---

## 🎯 Summary: What Goes Where

**Your Local Machine:**
- Source code
- Build Docker images
- Push to Docker Hub

**AWS Instances:**
- Only docker-compose.yml files
- Only .env file
- Pull images from Docker Hub
- Run docker-compose

**No git clone needed. No source code on AWS!** 🚀

---

**Ready? Start by building and pushing images from your local machine!**
