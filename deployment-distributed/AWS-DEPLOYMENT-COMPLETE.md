# ☁️ AWS Deployment Complete Guide

Deploy your distributed chat application to 6 AWS Lightsail instances.

---

## 📋 Prerequisites

✅ **Before Starting:**
- [ ] All Docker images built locally
- [ ] All images pushed to Docker Hub
- [ ] 6 AWS Lightsail instances created with static IPs
- [ ] SSH key (.pem file) downloaded
- [ ] Static IPs configured in `.env` file

**Your AWS Instances:**
```
Instance 1: 54.217.247.163  (1GB)  - Eureka + API Gateway
Instance 2: 35.153.96.103   (2GB)  - User + Message Services
Instance 3: 3.147.141.101   (1GB)  - MySQL Database
Instance 4: 3.147.109.193   (2GB)  - Kafka Message Broker
Instance 5: 98.89.238.241   (512MB)- Redis Cache
Instance 6: 54.154.129.84   (1GB)  - Frontend
```

---

## 📦 Step 1: Prepare Deployment Package

### Create Deployment Files Only (No Source Code)

```powershell
cd "C:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app"

# Create deployment package folder
New-Item -ItemType Directory -Path "aws-deployment" -Force

# Copy instance folders (only docker-compose.yml files)
Copy-Item -Path "deployment-distributed\instance-1-eureka-gateway\docker-compose.yml" -Destination "aws-deployment\instance-1-docker-compose.yml"
Copy-Item -Path "deployment-distributed\instance-2-user-message-services\docker-compose.yml" -Destination "aws-deployment\instance-2-docker-compose.yml"
Copy-Item -Path "deployment-distributed\instance-3-mysql\docker-compose.yml" -Destination "aws-deployment\instance-3-docker-compose.yml"
Copy-Item -Path "deployment-distributed\instance-4-kafka\docker-compose.yml" -Destination "aws-deployment\instance-4-docker-compose.yml"
Copy-Item -Path "deployment-distributed\instance-5-redis\docker-compose.yml" -Destination "aws-deployment\instance-5-docker-compose.yml"
Copy-Item -Path "deployment-distributed\instance-6-frontend\docker-compose.yml" -Destination "aws-deployment\instance-6-docker-compose.yml"

# Copy shared .env file
Copy-Item -Path "deployment-distributed\shared-config\.env" -Destination "aws-deployment\.env"

# Copy firewall rules
Copy-Item -Path "deployment-distributed\shared-config\FIREWALL-RULES.txt" -Destination "aws-deployment\FIREWALL-RULES.txt"
```

**Result**: `aws-deployment/` folder with only configuration files (no source code!)

---

## 🔧 Step 2: Setup AWS Instances (Do Once Per Instance)

### Save as `setup-instance.sh`:

```bash
#!/bin/bash
# AWS Lightsail Instance Setup Script

echo "🔧 Setting up AWS Lightsail instance..."

# Update system
sudo dnf update -y

# Install Docker
sudo dnf install docker -y
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify installations
echo "✅ Docker version:"
docker --version
echo "✅ Docker Compose version:"
docker-compose --version

echo "✅ Setup complete! Log out and back in to apply docker group."
```

### Upload and Run Setup on Each Instance:

```powershell
# Replace with your .pem file path
$KEY = "C:\path\to\your-key.pem"

# Setup Instance 1
scp -i $KEY setup-instance.sh ec2-user@54.217.247.163:~/
ssh -i $KEY ec2-user@54.217.247.163 'bash setup-instance.sh'

# Setup Instance 2
scp -i $KEY setup-instance.sh ec2-user@35.153.96.103:~/
ssh -i $KEY ec2-user@35.153.96.103 'bash setup-instance.sh'

# Setup Instance 3
scp -i $KEY setup-instance.sh ec2-user@3.147.141.101:~/
ssh -i $KEY ec2-user@3.147.141.101 'bash setup-instance.sh'

# Setup Instance 4
scp -i $KEY setup-instance.sh ec2-user@3.147.109.193:~/
ssh -i $KEY ec2-user@3.147.109.193 'bash setup-instance.sh'

# Setup Instance 5
scp -i $KEY setup-instance.sh ec2-user@98.89.238.241:~/
ssh -i $KEY ec2-user@98.89.238.241 'bash setup-instance.sh'

# Setup Instance 6
scp -i $KEY setup-instance.sh ec2-user@54.154.129.84:~/
ssh -i $KEY ec2-user@54.154.129.84 'bash setup-instance.sh'
```

### Reconnect (to apply docker group):

```powershell
# Test Docker works without sudo on Instance 1
ssh -i $KEY ec2-user@54.217.247.163 'docker ps'
```

---

## 📤 Step 3: Upload Deployment Files to AWS

### Upload to Each Instance:

```powershell
$KEY = "C:\path\to\your-key.pem"

# Instance 1: Eureka + Gateway
scp -i $KEY aws-deployment/instance-1-docker-compose.yml ec2-user@54.217.247.163:~/docker-compose.yml
scp -i $KEY aws-deployment/.env ec2-user@54.217.247.163:~/.env



# Instance 2: User + Message Services
scp -i $KEY aws-deployment/instance-2-docker-compose.yml ec2-user@35.153.96.103:~/docker-compose.yml
scp -i $KEY aws-deployment/.env ec2-user@35.153.96.103:~/.env

# Instance 3: MySQL
scp -i $KEY aws-deployment/instance-3-docker-compose.yml ec2-user@3.147.141.101:~/docker-compose.yml
scp -i $KEY aws-deployment/.env ec2-user@3.147.141.101:~/.env

# Instance 4: Kafka
scp -i $KEY aws-deployment/instance-4-docker-compose.yml ec2-user@3.147.109.193:~/docker-compose.yml
scp -i $KEY aws-deployment/.env ec2-user@3.147.109.193:~/.env

# Instance 5: Redis
scp -i $KEY aws-deployment/instance-5-docker-compose.yml ec2-user@98.89.238.241:~/docker-compose.yml
scp -i $KEY aws-deployment/.env ec2-user@98.89.238.241:~/.env

# Instance 6: Frontend
scp -i $KEY aws-deployment/instance-6-docker-compose.yml ec2-user@54.154.129.84:~/docker-compose.yml
scp -i $KEY aws-deployment/.env ec2-user@54.154.129.84:~/.env
```

---

## 🔒 Step 4: Configure AWS Firewalls

Open AWS Lightsail Console → Each Instance → Networking Tab → Firewall

### Instance 1 (Eureka + Gateway): 54.217.247.163
```
Application: Custom, Protocol: TCP, Port: 8761, Source: Anywhere (0.0.0.0/0)
Application: Custom, Protocol: TCP, Port: 8082, Source: Anywhere (0.0.0.0/0)
Application: SSH, Protocol: TCP, Port: 22, Source: Anywhere (0.0.0.0/0)
```

### Instance 2 (Services): 35.153.96.103
```
Application: Custom, Protocol: TCP, Port: 8081, Source: 54.217.247.163/32
Application: Custom, Protocol: TCP, Port: 8083, Source: 54.217.247.163/32
Application: SSH, Protocol: TCP, Port: 22, Source: Anywhere (0.0.0.0/0)
```

### Instance 3 (MySQL): 3.147.141.101
```
Application: Custom, Protocol: TCP, Port: 3306, Source: 35.153.96.103/32
Application: SSH, Protocol: TCP, Port: 22, Source: Anywhere (0.0.0.0/0)
```

### Instance 4 (Kafka): 3.147.109.193
```
Application: Custom, Protocol: TCP, Port: 9092, Source: 35.153.96.103/32
Application: SSH, Protocol: TCP, Port: 22, Source: Anywhere (0.0.0.0/0)
```

### Instance 5 (Redis): 98.89.238.241
```
Application: Custom, Protocol: TCP, Port: 6379, Source: 35.153.96.103/32
Application: SSH, Protocol: TCP, Port: 22, Source: Anywhere (0.0.0.0/0)
```

### Instance 6 (Frontend): 54.154.129.84
```
Application: HTTP, Protocol: TCP, Port: 80, Source: Anywhere (0.0.0.0/0)
Application: HTTPS, Protocol: TCP, Port: 443, Source: Anywhere (0.0.0.0/0)
Application: SSH, Protocol: TCP, Port: 22, Source: Anywhere (0.0.0.0/0)
```

---

## 🚀 Step 5: Deploy Services (In Correct Order)

### 5.1 Deploy MySQL (Instance 3)

```powershell
$KEY = "C:\path\to\your-key.pem"
ssh -i $KEY ec2-user@3.147.141.101
```

On the instance:
```bash
# Pull and start MySQL
docker-compose pull
docker-compose up -d

# Wait 15 seconds
sleep 15

# Verify
docker-compose ps
docker-compose logs mysql

# Test connection
docker exec -it mysql mysql -uroot -prootpassword -e "SELECT 1;"

# Exit SSH
exit
```

**✅ Expected**: MySQL container running, accepting connections

---

### 5.2 Deploy Kafka (Instance 4)

```powershell
ssh -i $KEY ec2-user@3.147.109.193
```

On the instance:
```bash
# Pull and start Kafka
docker-compose pull
docker-compose up -d

# Wait 30 seconds for Kafka to initialize
sleep 30

# Verify
docker-compose ps
docker-compose logs kafka

# Test Kafka
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Exit SSH
exit
```

**✅ Expected**: Kafka container running, no topics yet (that's OK)

---

### 5.3 Deploy Redis (Instance 5)

```powershell
ssh -i $KEY ec2-user@98.89.238.241
```

On the instance:
```bash
# Pull and start Redis
docker-compose pull
docker-compose up -d

# Verify
docker-compose ps
docker-compose logs redis

# Test Redis
docker exec -it redis redis-cli ping

# Exit SSH
exit
```

**✅ Expected**: Redis responds with "PONG"

---

### 5.4 Deploy Eureka + Gateway (Instance 1)

```powershell
ssh -i $KEY ec2-user@54.217.247.163
```

On the instance:
```bash
# Pull images from Docker Hub
docker-compose pull

# Start services
docker-compose up -d

# Wait 60 seconds for Eureka to start
sleep 60

# Verify
docker-compose ps
docker-compose logs eureka-server
docker-compose logs api-gateway

# Test health endpoints
curl http://localhost:8761/actuator/health
curl http://localhost:8082/actuator/health

# Exit SSH
exit
```

**✅ Test from your machine**:
```powershell
# Open Eureka Dashboard
Start-Process "http://54.217.247.163:8761"
```

**✅ Expected**: Eureka Dashboard loads, currently no services registered

---

### 5.5 Deploy User + Message Services (Instance 2)

```powershell
ssh -i $KEY ec2-user@35.153.96.103
```

On the instance:
```bash
# Pull images from Docker Hub
docker-compose pull

# Start services
docker-compose up -d

# Wait 60 seconds for services to register with Eureka
sleep 60

# Verify
docker-compose ps
docker-compose logs user-service
docker-compose logs message-service

# Test health
curl http://localhost:8081/actuator/health
curl http://localhost:8083/actuator/health

# Exit SSH
exit
```

**✅ Test from your machine**:
```powershell
# Refresh Eureka Dashboard
Start-Process "http://54.217.247.163:8761"
```

**✅ Expected**: 
- USER-SERVICE registered in Eureka
- MESSAGE-SERVICE registered in Eureka
- Both showing UP status

---

### 5.6 Deploy Frontend (Instance 6)

```powershell
ssh -i $KEY ec2-user@54.154.129.84
```

On the instance:
```bash
# Pull image from Docker Hub
docker-compose pull

# Start frontend
docker-compose up -d

# Verify
docker-compose ps
docker-compose logs frontend

# Test locally
curl http://localhost/

# Exit SSH
exit
```

**✅ Test from your machine**:
```powershell
# Open frontend
Start-Process "http://54.154.129.84"
```

**✅ Expected**: Chat application loads in browser

---

## ✅ Step 6: End-to-End Testing

### 6.1 Check All Services in Eureka

Visit: http://54.217.247.163:8761

**Expected Registered Services:**
- ✅ API-GATEWAY (1 instance)
- ✅ USER-SERVICE (1 instance)
- ✅ MESSAGE-SERVICE (1 instance)

### 6.2 Test Application Flow

1. **Open Frontend**: http://54.154.129.84

2. **Register New User**:
   - Click "Register"
   - Username: `testuser`
   - Email: `test@example.com`
   - Password: `Test@123`
   - Click Submit

3. **Login**:
   - Enter credentials
   - Click Login

4. **Send Message**:
   - Select a user
   - Type: "Hello from AWS!"
   - Click Send

5. **Verify**:
   - Message appears in chat
   - No errors in browser console

### 6.3 Verify Backend Communication

```powershell
# Test via Gateway (from your machine)
curl http://54.217.247.163:8082/actuator/health

# Test User Service (through Gateway)
curl -X GET http://54.217.247.163:8082/api/users/health
```

---

## 📊 Step 7: Monitor Services

### Check All Instances

Save as `check-all.ps1`:

```powershell
$KEY = "C:\path\to\your-key.pem"

Write-Host "`n🔍 Instance 1: Eureka + Gateway" -ForegroundColor Cyan
ssh -i $KEY ec2-user@54.217.247.163 'docker ps && docker stats --no-stream'

Write-Host "`n🔍 Instance 2: Services" -ForegroundColor Cyan
ssh -i $KEY ec2-user@35.153.96.103 'docker ps && docker stats --no-stream'

Write-Host "`n🔍 Instance 3: MySQL" -ForegroundColor Cyan
ssh -i $KEY ec2-user@3.147.141.101 'docker ps && docker stats --no-stream'

Write-Host "`n🔍 Instance 4: Kafka" -ForegroundColor Cyan
ssh -i $KEY ec2-user@3.147.109.193 'docker ps && docker stats --no-stream'

Write-Host "`n🔍 Instance 5: Redis" -ForegroundColor Cyan
ssh -i $KEY ec2-user@98.89.238.241 'docker ps && docker stats --no-stream'

Write-Host "`n🔍 Instance 6: Frontend" -ForegroundColor Cyan
ssh -i $KEY ec2-user@54.154.129.84 'docker ps && docker stats --no-stream'
```

### Check Specific Service Logs

```bash
# On any instance
docker-compose logs -f [service-name]
docker-compose logs --tail=100 [service-name]
```

### Check Database

```bash
# SSH to Instance 3
ssh ec2-user@3.147.141.101

# Check users table
docker exec -it mysql mysql -uroot -prootpassword chatapp -e "SELECT * FROM users;"

# Check messages table
docker exec -it mysql mysql -uroot -prootpassword chatapp -e "SELECT COUNT(*) FROM messages;"
```

### Check Kafka Topics

```bash
# SSH to Instance 4
ssh ec2-user@3.147.109.193

# List topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Check message topic
docker exec -it kafka kafka-topics --describe --topic chat-messages --bootstrap-server localhost:9092
```

### Check Redis Cache

```bash
# SSH to Instance 5
ssh ec2-user@98.89.238.241

# Check Redis keys
docker exec -it redis redis-cli KEYS '*'

# Check Redis info
docker exec -it redis redis-cli INFO stats
```

---

## 🔄 Step 8: Update Deployment (When Code Changes)

### Update Single Service:

```powershell
# 1. Build new image locally
cd "C:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app"
cd deployment-distributed\instance-2-user-message-services
docker build -f Dockerfile.user -t rakes9146/chat-user-service:distributed ..\..\

# 2. Push to Docker Hub
docker push rakes9146/chat-user-service:distributed

# 3. Update on AWS
$KEY = "C:\path\to\your-key.pem"
ssh -i $KEY ec2-user@35.153.96.103
docker-compose pull user-service
docker-compose up -d user-service
docker-compose logs -f user-service
exit
```

### Update All Services:

```powershell
# 1. Rebuild all locally (use build-all.ps1)
.\build-all.ps1

# 2. Push all (use push-all.ps1)
docker login
.\push-all.ps1

# 3. Update on AWS
$KEY = "C:\path\to\your-key.pem"

# Update Instance 1
ssh -i $KEY ec2-user@54.217.247.163 'docker-compose pull && docker-compose up -d'

# Update Instance 2
ssh -i $KEY ec2-user@35.153.96.103 'docker-compose pull && docker-compose up -d'

# Update Instance 6
ssh -i $KEY ec2-user@54.154.129.84 'docker-compose pull && docker-compose up -d'
```

---

## 🛑 Step 9: Stop/Restart Services

### Stop Single Service:

```bash
# SSH to instance
docker-compose stop [service-name]

# Restart
docker-compose start [service-name]
```

### Stop All Services on Instance:

```bash
docker-compose down

# Start again
docker-compose up -d
```

### Restart Order (if all stopped):

1. MySQL (Instance 3)
2. Kafka (Instance 4)
3. Redis (Instance 5)
4. Eureka + Gateway (Instance 1) - Wait 60s
5. Services (Instance 2) - Wait 60s
6. Frontend (Instance 6)

---

## 🧹 Step 10: Clean Up (Remove Everything)

### Remove Containers (Keep Images):

```bash
# On each instance
docker-compose down
```

### Remove Everything (Containers + Volumes + Images):

```bash
# On each instance
docker-compose down -v
docker system prune -a -f
```

---

## 🐛 Troubleshooting

### Issue 1: Service Won't Start

```bash
# Check logs
docker-compose logs [service-name]

# Check if port is in use
sudo netstat -tlnp | grep [port]

# Restart
docker-compose restart [service-name]

# Force recreate
docker-compose up -d --force-recreate [service-name]
```

### Issue 2: Can't Connect to Other Services

```bash
# Check if target service is running
ssh ec2-user@[target-ip] 'docker ps'

# Test network connectivity
docker exec -it [container] ping [target-ip]

# Check firewall rules in AWS Console
```

### Issue 3: Service Not Registering in Eureka

```bash
# Check Eureka is accessible
curl http://54.217.247.163:8761/actuator/health

# Check service environment variables
docker exec -it [container] env | grep EUREKA

# Restart service
docker-compose restart [service-name]
```

### Issue 4: Out of Memory

```bash
# Check memory usage
docker stats --no-stream

# Restart Docker
sudo systemctl restart docker

# Increase instance size in AWS Lightsail
```

### Issue 5: Image Pull Failed

```bash
# Check Docker Hub
curl https://hub.docker.com/v2/repositories/rakes9146/chat-user-service/tags/distributed

# Manual pull
docker pull rakes9146/chat-user-service:distributed

# Login to Docker Hub (if private repo)
docker login
```

---

## 📋 Deployment Checklist

```
Pre-Deployment:
☐ All images built locally
☐ All images pushed to Docker Hub  
☐ 6 AWS Lightsail instances created
☐ Static IPs assigned
☐ SSH key downloaded
☐ .env file updated with IPs

Instance Setup:
☐ Docker installed on all instances
☐ Docker Compose installed on all instances
☐ Deployment files uploaded to all instances
☐ Firewall rules configured in AWS

Service Deployment:
☐ MySQL deployed and verified
☐ Kafka deployed and verified
☐ Redis deployed and verified
☐ Eureka + Gateway deployed and verified
☐ User + Message services deployed and verified
☐ Frontend deployed and verified

Testing:
☐ All services registered in Eureka
☐ Health endpoints responding
☐ User registration works
☐ User login works
☐ Message sending works
☐ Messages received in real-time

Monitoring:
☐ All containers running
☐ Resource usage acceptable
☐ No errors in logs
☐ Application accessible from browser
```

---

## 🎯 Quick Commands Reference

```bash
# Check status
docker-compose ps
docker ps
docker stats --no-stream

# View logs
docker-compose logs -f
docker-compose logs --tail=100 [service]

# Restart service
docker-compose restart [service]

# Update service
docker-compose pull [service]
docker-compose up -d [service]

# Stop all
docker-compose down

# Start all
docker-compose up -d

# Check health
curl http://localhost:[port]/actuator/health
```

---

## 📞 Support URLs

- **Eureka Dashboard**: http://54.217.247.163:8761
- **API Gateway Health**: http://54.217.247.163:8082/actuator/health
- **Frontend**: http://54.154.129.84
- **Docker Hub**: https://hub.docker.com/u/rakes9146

---

**🎉 Deployment Complete!** Your distributed chat application is now running on AWS! 🚀
