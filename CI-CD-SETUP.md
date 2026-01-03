# 🚀 CI/CD Pipeline Setup Guide

## Complete Setup Instructions for AWS Lightsail Deployment

---

## 📋 Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [GitHub Repository Setup](#github-repository-setup)
4. [Lightsail Instance Setup](#lightsail-instance-setup)
5. [Running Your First Deployment](#running-your-first-deployment)
6. [Rollback Procedure](#rollback-procedure)
7. [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

### What This Pipeline Does:
1. **Builds** all Docker images from source code
2. **Pushes** images to Docker Hub with version tags
3. **Deploys** to 3 Lightsail instances in sequence:
   - Instance 1: Eureka + API Gateway
   - Instance 2: User + Message Services
   - Instance 6: Frontend
4. **Verifies** each service is healthy after deployment

### What It Doesn't Touch:
- ❌ MySQL (Instance 3) - Infrastructure service
- ❌ Kafka (Instance 4) - Infrastructure service
- ❌ Redis (Instance 5) - Infrastructure service

---

## ✅ Prerequisites

### 1. GitHub Account
- Repository: `rakes9146/basic-chat-app`
- Admin access to repository settings

### 2. Docker Hub Account
- Username: `rakes9146`
- Access token created (for CI/CD)

### 3. AWS Lightsail Instances (Already Running)
- Instance 1: 54.217.247.163 (Eureka + Gateway)
- Instance 2: 35.153.96.103 (User + Message)
- Instance 3: 3.147.141.101 (MySQL)
- Instance 4: 3.147.109.193 (Kafka)
- Instance 5: 98.89.238.241 (Redis)
- Instance 6: 54.154.129.84 (Frontend)

---

## 🔧 Step 1: GitHub Repository Setup

### 1.1 Add GitHub Secrets

Go to your repository: `https://github.com/rakes9146/basic-chat-app/settings/secrets/actions`

Click **"New repository secret"** and add each of the following:

#### **DOCKER_USERNAME**
```
rakes9146
```

#### **DOCKER_PASSWORD**
```
<your-docker-hub-access-token>
```

**How to get Docker Hub Access Token:**
1. Go to https://hub.docker.com/settings/security
2. Click "New Access Token"
3. Name: `github-actions-ci-cd`
4. Access permissions: **Read, Write, Delete**
5. Copy the token (you won't see it again!)

#### **LIGHTSAIL_SSH_KEY**
```
<your-lightsail-private-key>
```

**How to get Lightsail SSH Key:**
1. Go to AWS Lightsail Console
2. Account → SSH Keys
3. Download your default key (e.g., `LightsailDefaultKey-us-east-1.pem`)
4. Open the file in a text editor
5. Copy the ENTIRE content including:
   ```
   -----BEGIN RSA PRIVATE KEY-----
   ...
   -----END RSA PRIVATE KEY-----
   ```

#### **INSTANCE_1_IP**
```
54.217.247.163
```

#### **INSTANCE_2_IP**
```
35.153.96.103
```

#### **INSTANCE_6_IP**
```
54.154.129.84
```

### 1.2 Verify Secrets
Your GitHub secrets should look like this:
```
✅ DOCKER_USERNAME
✅ DOCKER_PASSWORD
✅ LIGHTSAIL_SSH_KEY
✅ INSTANCE_1_IP
✅ INSTANCE_2_IP
✅ INSTANCE_6_IP
```

---

## 🖥️ Step 2: Lightsail Instance Setup

### 2.1 SSH into Each Instance

For each instance (1, 2, and 6), you need to:

#### Connect to Instance:
```bash
# For Instance 1
ssh -i "LightsailDefaultKey-us-east-1.pem" ubuntu@54.217.247.163

# For Instance 2
ssh -i "LightsailDefaultKey-us-east-1.pem" ubuntu@35.153.96.103

# For Instance 6
ssh -i "LightsailDefaultKey-us-east-1.pem" ubuntu@54.154.129.84
```

### 2.2 Create Deployment Directory Structure

#### **On Instance 1:**
```bash
# Create deployment directory
mkdir -p ~/deployment/instance-1-eureka-gateway
cd ~/deployment/instance-1-eureka-gateway

# Copy docker-compose.yml from your existing setup
# (Use the one from deployment-distributed/instance-1-eureka-gateway/)

# Make deploy script executable
chmod +x ~/deployment/instance-1-eureka-gateway/deploy.sh
```

#### **On Instance 2:**
```bash
# Create deployment directory
mkdir -p ~/deployment/instance-2-user-message-services
cd ~/deployment/instance-2-user-message-services

# Copy docker-compose.yml from your existing setup
# (Use the one from deployment-distributed/instance-2-user-message-services/)

# Make deploy script executable
chmod +x ~/deployment/instance-2-user-message-services/deploy.sh
```

#### **On Instance 6:**
```bash
# Create deployment directory
mkdir -p ~/deployment/instance-6-frontend
cd ~/deployment/instance-6-frontend

# Copy docker-compose.yml from your existing setup
# (Use the one from deployment-distributed/instance-6-frontend/)

# Make deploy script executable
chmod +x ~/deployment/instance-6-frontend/deploy.sh
```

### 2.3 Copy Required Files to Instances

You need to copy these files to each instance:

#### **Instance 1 files:**
```bash
# From your local machine:
cd "C:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app"

# Copy docker-compose.yml
scp -i "LightsailDefaultKey-us-east-1.pem" deployment-distributed/instance-1-eureka-gateway/docker-compose.yml ubuntu@54.217.247.163:~/deployment/instance-1-eureka-gateway/

# Copy .env file (if you have one)
scp -i "LightsailDefaultKey-us-east-1.pem" deployment-distributed/instance-1-eureka-gateway/.env ubuntu@54.217.247.163:~/deployment/instance-1-eureka-gateway/
```

#### **Instance 2 files:**
```bash
# Copy docker-compose.yml
scp -i "LightsailDefaultKey-us-east-1.pem" deployment-distributed/instance-2-user-message-services/docker-compose.yml ubuntu@35.153.96.103:~/deployment/instance-2-user-message-services/

# Copy .env file (if you have one)
scp -i "LightsailDefaultKey-us-east-1.pem" deployment-distributed/instance-2-user-message-services/.env ubuntu@35.153.96.103:~/deployment/instance-2-user-message-services/
```

#### **Instance 6 files:**
```bash
# Copy docker-compose.yml
scp -i "LightsailDefaultKey-us-east-1.pem" deployment-distributed/instance-6-frontend/docker-compose.yml ubuntu@54.154.129.84:~/deployment/instance-6-frontend/

# Copy .env file (if you have one)
scp -i "LightsailDefaultKey-us-east-1.pem" deployment-distributed/instance-6-frontend/.env ubuntu@54.154.129.84:~/deployment/instance-6-frontend/
```

### 2.4 Verify Setup on Each Instance

SSH into each instance and verify:

```bash
# Check directory structure
ls -la ~/deployment/

# Check docker-compose.yml exists
cat ~/deployment/instance-1-eureka-gateway/docker-compose.yml

# Verify Docker is installed
docker --version
docker-compose --version
```

---

## 🚀 Step 3: Running Your First Deployment

### 3.1 Manual Deployment via GitHub Actions

1. **Go to GitHub Actions**:
   ```
   https://github.com/rakes9146/basic-chat-app/actions
   ```

2. **Select "Deploy to Production" workflow** from left sidebar

3. **Click "Run workflow" button** (top right)

4. **Fill in the form**:
   - **Branch**: `main` (or your current branch)
   - **Version tag**: Leave empty for commit SHA, or enter version like `v1.0.0`

5. **Click green "Run workflow" button**

6. **Watch the deployment**:
   - Click on the running workflow
   - Watch each job execute in real-time
   - See logs for each step

### 3.2 Deployment Process

The workflow will:

```
1. Build Stage (5-10 min)
   ├─ Build Eureka Server image
   ├─ Build API Gateway image
   ├─ Build User Service image
   ├─ Build Message Service image
   └─ Build Frontend image
   
2. Push Stage (2-3 min)
   └─ Push all images to Docker Hub

3. Deploy Instance 1 (1-2 min)
   ├─ SSH to Instance 1
   ├─ Pull new images
   ├─ Restart containers
   └─ Verify health

4. Deploy Instance 2 (1-2 min)
   ├─ SSH to Instance 2
   ├─ Pull new images
   ├─ Restart containers
   └─ Verify health

5. Deploy Instance 6 (1 min)
   ├─ SSH to Instance 6
   ├─ Pull new image
   ├─ Restart container
   └─ Verify health
```

**Total time: ~10-15 minutes**

### 3.3 Verify Deployment Success

After deployment completes:

1. **Check GitHub Actions Summary** - Shows deployment status
2. **Test Services Manually**:

```bash
# Eureka Dashboard
http://54.217.247.163:8761

# API Gateway Health
http://54.217.247.163:8082/actuator/health

# Frontend
http://54.154.129.84

# User Service Health
http://35.153.96.103:8081/actuator/health

# Message Service Health
http://35.153.96.103:8083/actuator/health
```

---

## 🔄 Step 4: Rollback Procedure

If something goes wrong, you can rollback to a previous version.

### 4.1 Find Previous Version

Check Docker Hub for available versions:
```
https://hub.docker.com/r/rakes9146/chat-eureka/tags
```

Or check your GitHub Actions history for commit SHAs.

### 4.2 Execute Rollback

1. Go to GitHub Actions
2. Select **"Rollback Deployment"** workflow
3. Click **"Run workflow"**
4. Fill in:
   - **Version tag to rollback to**: (e.g., `abc123def` or `v1.0.0`)
   - **Instances to rollback**: `all` (or specific: `1,2,6`)
5. Click **"Run workflow"**

### 4.3 Rollback Process

```
1. Validate Version
   └─ Check if version exists in Docker Hub

2. Rollback Instance 1
   ├─ Pull old version
   ├─ Tag as latest
   ├─ Restart containers
   └─ Verify health

3. Rollback Instance 2
   ├─ Pull old version
   ├─ Tag as latest
   ├─ Restart containers
   └─ Verify health

4. Rollback Instance 6
   ├─ Pull old version
   ├─ Tag as latest
   ├─ Restart container
   └─ Verify health
```

---

## 🐛 Troubleshooting

### Issue 1: "Permission denied (publickey)"

**Problem**: SSH connection fails

**Solution**:
```bash
# Check SSH key has correct permissions
chmod 400 LightsailDefaultKey-us-east-1.pem

# Verify you can SSH manually
ssh -i "LightsailDefaultKey-us-east-1.pem" ubuntu@54.217.247.163

# If this works, your GitHub secret might be incorrect
# Make sure you copied the ENTIRE private key including headers
```

### Issue 2: "Docker image not found"

**Problem**: Deployment fails with image pull error

**Solution**:
```bash
# Check Docker Hub credentials
# Verify DOCKER_USERNAME and DOCKER_PASSWORD secrets

# Check if images were pushed
https://hub.docker.com/r/rakes9146/

# If images exist, check instance can pull
ssh ubuntu@54.217.247.163
docker pull rakes9146/chat-eureka:latest
```

### Issue 3: "Health check failed"

**Problem**: Service doesn't respond after deployment

**Solution**:
```bash
# SSH to the instance
ssh ubuntu@54.217.247.163

# Check container logs
cd ~/deployment/instance-1-eureka-gateway
docker-compose logs -f

# Check if containers are running
docker-compose ps

# Check if port is accessible
curl localhost:8761
```

### Issue 4: "Services can't connect to MySQL/Kafka/Redis"

**Problem**: Backend services fail to connect to infrastructure

**Solution**:
```bash
# Verify infrastructure services are running
ssh ubuntu@3.147.141.101  # MySQL
docker ps

ssh ubuntu@3.147.109.193  # Kafka
docker ps

ssh ubuntu@98.89.238.241  # Redis
docker ps

# Check firewall rules allow connections
# Verify .env files have correct IPs
```

### Issue 5: "Workflow stuck or timeout"

**Problem**: GitHub Actions workflow hangs

**Solution**:
1. Cancel the workflow
2. SSH to the instance manually
3. Check what's wrong:
   ```bash
   docker-compose ps
   docker-compose logs
   ```
4. Fix the issue
5. Re-run the workflow

---

## 📝 Best Practices

### 1. Environment Variables
- ✅ Keep `.env` files on instances (not in Git)
- ✅ Use GitHub Secrets for CI/CD credentials
- ✅ Keep `.env.example` in Git as template

### 2. Deployment Frequency
- Deploy during low-traffic hours
- Test in dev environment first (if you have one)
- Keep rollback window ready

### 3. Monitoring
- Check logs after each deployment
- Monitor service health endpoints
- Keep recent Docker image versions (last 3-5)

### 4. Version Tagging
- Use semantic versioning: `v1.0.0`, `v1.0.1`, etc.
- Tag major releases
- Commit SHA is fine for frequent deployments

### 5. Database Migrations
- Run database migrations manually (for now)
- Never auto-restart MySQL during deployments
- Backup before schema changes

---

## 🎉 Success Checklist

After setup, you should be able to:

- [ ] View deployment workflow in GitHub Actions
- [ ] Click "Run workflow" button
- [ ] Watch deployment progress in real-time
- [ ] See all services update automatically
- [ ] Access updated application via frontend
- [ ] Rollback to previous version if needed
- [ ] View deployment history and logs

---

## 📞 Quick Reference

### GitHub Actions URLs
- **Workflows**: `https://github.com/rakes9146/basic-chat-app/actions`
- **Secrets**: `https://github.com/rakes9146/basic-chat-app/settings/secrets/actions`

### Docker Hub URLs
- **Images**: `https://hub.docker.com/u/rakes9146`
- **Tags**: `https://hub.docker.com/r/rakes9146/chat-eureka/tags`

### Service URLs
| Service | URL |
|---------|-----|
| Frontend | http://54.154.129.84 |
| API Gateway | http://54.217.247.163:8082 |
| Eureka Dashboard | http://54.217.247.163:8761 |
| User Service | http://35.153.96.103:8081 |
| Message Service | http://35.153.96.103:8083 |

### SSH Commands
```bash
# Instance 1 (Eureka + Gateway)
ssh -i "LightsailDefaultKey-us-east-1.pem" ubuntu@54.217.247.163

# Instance 2 (User + Message)
ssh -i "LightsailDefaultKey-us-east-1.pem" ubuntu@35.153.96.103

# Instance 6 (Frontend)
ssh -i "LightsailDefaultKey-us-east-1.pem" ubuntu@54.154.129.84
```

---

## 🚀 What's Next?

Consider adding these enhancements:

1. **Automatic Deployments**
   - Deploy on push to `main` branch
   - Deploy on tag creation (`v*`)

2. **Testing Pipeline**
   - Add unit tests before deployment
   - Add integration tests
   - Add smoke tests after deployment

3. **Notifications**
   - Slack notifications on deployment success/failure
   - Email notifications for critical failures

4. **Blue-Green Deployment**
   - Zero-downtime deployments
   - Requires duplicate instances

5. **Database Migrations**
   - Automated schema migrations
   - Database backup before deployment

6. **Monitoring Integration**
   - Prometheus metrics
   - Grafana dashboards
   - Alerting on deployment issues

---

**🎊 Congratulations! Your CI/CD pipeline is ready!**

Now you can deploy with a single click and rollback just as easily. 🚀
