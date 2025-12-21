# Chat Application - Docker Deployment Guide

This guide provides complete instructions for deploying the chat application using Docker Compose, both locally and on AWS Lightsail.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Architecture Overview](#architecture-overview)
3. [Local Deployment](#local-deployment)
4. [AWS Lightsail Deployment](#aws-lightsail-deployment)
5. [Configuration](#configuration)
6. [Health Checks](#health-checks)
7. [Troubleshooting](#troubleshooting)
8. [Backup & Maintenance](#backup--maintenance)

## Prerequisites

### For Local Deployment
- Docker Desktop (Windows/Mac) or Docker Engine (Linux) - version 20.10+
- Docker Compose - version 2.0+
- At least 4GB RAM available for Docker
- 10GB free disk space

### For AWS Lightsail Deployment
- AWS account with Lightsail access
- SSH key pair for instance access
- Domain name (optional, for production)

## Architecture Overview

The application consists of 7 containerized services:

```
┌─────────────┐
│   Frontend  │ (Nginx:80) ──┐
└─────────────┘              │
                              ↓
┌─────────────┐         ┌──────────────┐
│   Eureka    │ ←───────│ API Gateway  │ (8082)
└─────────────┘         └──────────────┘
     (8761)                    │
       ↑                       │
       │              ┌────────┴─────────┐
       │              │                  │
       │        ┌──────────┐      ┌─────────────┐
       └────────│  User    │      │  Message    │
                │ Service  │      │  Service    │
                └──────────┘      └─────────────┘
                     │                   │
                     │                   ├─── WebSocket (8084)
                     │                   │
                ┌────┴──────────┬────────┴─────┐
                │               │              │
            ┌───────┐      ┌────────┐    ┌────────┐
            │ MySQL │      │ Kafka  │    │ Redis  │
            └───────┘      └────────┘    └────────┘
             (3306)         (9092)        (6379)
```

### Services:
1. **MySQL** - Persistent database for users and messages
2. **Kafka** - Message queue in KRaft mode (no ZooKeeper)
3. **Redis** - Session and presence management
4. **Eureka Server** - Service discovery (port 8761)
5. **API Gateway** - Spring Cloud Gateway (port 8082)
6. **User Service** - User management microservice
7. **Message Service** - Real-time messaging with WebSocket
8. **Frontend** - Angular app served by Nginx (port 80)

## Local Deployment

### Step 1: Clone and Setup

```powershell
# Navigate to project directory
cd "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app"

# Create .env file from example
Copy-Item .env.example .env
```

### Step 2: Configure Environment

Edit `.env` file with your local settings:

```env
# MySQL Configuration
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=chatdb
MYSQL_USER=chatuser
MYSQL_PASSWORD=chatpass
MYSQL_PORT=3306

# Kafka Configuration
KAFKA_PORT=9092

# Redis Configuration
REDIS_PORT=6379

# Eureka Configuration
EUREKA_PORT=8761

# API Gateway Configuration
GATEWAY_PORT=8082

# Frontend Configuration
FRONTEND_PORT=80

# Spring Profile
SPRING_PROFILES_ACTIVE=docker

# Application Settings
APP_ENVIRONMENT=development
```

### Step 3: Build and Start Services

```powershell
# Build all images (first time or after code changes)
docker-compose build

# Start all services
docker-compose up -d

# View logs (optional)
docker-compose logs -f

# View specific service logs
docker-compose logs -f message-service
```

### Step 4: Verify Deployment

```powershell
# Check all services are running
docker-compose ps

# Test infrastructure services
docker-compose exec mysql mysql -uchatuser -pchatpass -e "SHOW DATABASES;"
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Check service health
curl http://localhost:8761  # Eureka Dashboard
curl http://localhost:8082/actuator/health  # API Gateway
curl http://localhost  # Frontend
```

### Step 5: Access Application

- **Frontend**: http://localhost
- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8082

## AWS Lightsail Deployment

### Step 1: Create Lightsail Instance

1. Log in to AWS Console and navigate to Lightsail
2. Click "Create Instance"
3. Select:
   - **Platform**: Linux/Unix
   - **Blueprint**: OS Only → Ubuntu 22.04 LTS
   - **Instance Plan**: At least 2GB RAM (recommended: 4GB or 8GB)
   - **Instance Name**: chat-app-server
4. Wait for instance to be running and note the public IP

### Step 2: Configure Firewall

In Lightsail console, go to Networking tab and add these rules:

| Application | Protocol | Port Range | Source     |
|-------------|----------|------------|------------|
| HTTP        | TCP      | 80         | 0.0.0.0/0  |
| HTTPS       | TCP      | 443        | 0.0.0.0/0  |
| Custom      | TCP      | 8082       | 0.0.0.0/0  |
| Custom      | TCP      | 8761       | 0.0.0.0/0  |
| SSH         | TCP      | 22         | Your-IP/32 |

### Step 3: Connect and Install Docker

```bash
# Connect via SSH
ssh ubuntu@<your-lightsail-ip>

# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user to docker group
sudo usermod -aG docker ubuntu
newgrp docker

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verify installation
docker --version
docker-compose --version
```

### Step 4: Deploy Application

```bash
# Create application directory
mkdir -p ~/chat-app
cd ~/chat-app

# Upload files (from your local machine)
# Option A: Using SCP
scp -r "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\*" ubuntu@<your-ip>:~/chat-app/

# Option B: Using Git
git clone <your-repo-url> .

# Create production .env file
nano .env
```

Production `.env` configuration:

```env
# MySQL Configuration
MYSQL_ROOT_PASSWORD=<strong-random-password>
MYSQL_DATABASE=chatdb
MYSQL_USER=chatuser
MYSQL_PASSWORD=<strong-random-password>
MYSQL_PORT=3306

# Kafka Configuration
KAFKA_PORT=9092

# Redis Configuration
REDIS_PORT=6379

# Eureka Configuration
EUREKA_PORT=8761

# API Gateway Configuration
GATEWAY_PORT=8082

# Frontend Configuration
FRONTEND_PORT=80

# Spring Profile
SPRING_PROFILES_ACTIVE=production

# Application Settings
APP_ENVIRONMENT=production
```

### Step 5: Update Frontend Environment

For production deployment, update `frontend/src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiGateway: 'http://<your-lightsail-ip>:8082',
  userServiceUrl: 'http://<your-lightsail-ip>:8082/USER-SERVICE/user',
  messageServiceUrl: 'http://<your-lightsail-ip>:8082/MESSAGE-SERVICE/message',
  webSocketUrl: 'ws://<your-lightsail-ip>:8082/MESSAGE-SERVICE/ws'
};
```

Or use your domain name if configured:

```typescript
export const environment = {
  production: true,
  apiGateway: 'https://api.yourdomain.com',
  userServiceUrl: 'https://api.yourdomain.com/USER-SERVICE/user',
  messageServiceUrl: 'https://api.yourdomain.com/MESSAGE-SERVICE/message',
  webSocketUrl: 'wss://api.yourdomain.com/MESSAGE-SERVICE/ws'
};
```

### Step 6: Build and Deploy

```bash
# Build images
docker-compose build

# Start services in detached mode
docker-compose up -d

# Monitor logs
docker-compose logs -f
```

### Step 7: Verify Production Deployment

```bash
# Check service status
docker-compose ps

# Test services
curl http://localhost:8761  # Eureka
curl http://localhost:8082/actuator/health  # Gateway
curl http://localhost  # Frontend

# From external machine
curl http://<your-lightsail-ip>
```

## Configuration

### Environment Variables

All configuration is managed through the `.env` file:

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| MYSQL_ROOT_PASSWORD | MySQL root password | rootpassword | Yes |
| MYSQL_DATABASE | Database name | chatdb | Yes |
| MYSQL_USER | Application database user | chatuser | Yes |
| MYSQL_PASSWORD | Application database password | chatpass | Yes |
| MYSQL_PORT | MySQL port | 3306 | Yes |
| KAFKA_PORT | Kafka broker port | 9092 | Yes |
| REDIS_PORT | Redis port | 6379 | Yes |
| EUREKA_PORT | Eureka server port | 8761 | Yes |
| GATEWAY_PORT | API Gateway port | 8082 | Yes |
| FRONTEND_PORT | Frontend Nginx port | 80 | Yes |
| SPRING_PROFILES_ACTIVE | Spring profile | docker | Yes |
| APP_ENVIRONMENT | Environment name | production | No |

### Service Configuration

Each service has its own `application.properties` that reads from environment variables:

**User Service & Message Service:**
```properties
spring.datasource.url=jdbc:mysql://mysql:3306/${MYSQL_DATABASE}
spring.datasource.username=${MYSQL_USER}
spring.datasource.password=${MYSQL_PASSWORD}
```

**Message Service (Kafka):**
```properties
spring.kafka.bootstrap-servers=kafka:9092
```

**All Services (Eureka):**
```properties
eureka.client.serviceUrl.defaultZone=http://eureka:8761/eureka/
```

### Scaling Services

To scale services horizontally (not applicable for all):

```powershell
# Scale user-service to 3 instances
docker-compose up -d --scale user-service=3

# Scale message-service (be careful with WebSocket)
docker-compose up -d --scale message-service=2
```

**Note**: WebSocket services require sticky sessions for scaling.

## Health Checks

### Built-in Health Checks

MySQL and Kafka have health checks in docker-compose.yml:

```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
  interval: 10s
  timeout: 5s
  retries: 5
```

### Manual Health Checks

```powershell
# Check all containers
docker-compose ps

# Check specific service logs
docker-compose logs message-service

# Check MySQL
docker-compose exec mysql mysql -uchatuser -pchatpass -e "SELECT 1;"

# Check Kafka topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Check Redis
docker-compose exec redis redis-cli ping

# Spring Boot Actuator endpoints
curl http://localhost:8082/actuator/health
curl http://localhost:8761/actuator/health
```

### Service Dependencies

Services start in the following order:
1. MySQL, Kafka, Redis (infrastructure)
2. Eureka Server (service discovery)
3. User Service, Message Service (microservices)
4. API Gateway (routing)
5. Frontend (UI)

## Troubleshooting

### Common Issues

#### 1. Services Not Starting

```powershell
# Check service logs
docker-compose logs <service-name>

# Restart specific service
docker-compose restart <service-name>

# Rebuild and restart
docker-compose up -d --build <service-name>
```

#### 2. Database Connection Issues

```powershell
# Verify MySQL is running
docker-compose ps mysql

# Check MySQL logs
docker-compose logs mysql

# Test connection
docker-compose exec mysql mysql -uchatuser -pchatpass chatdb

# Reset database (WARNING: deletes data)
docker-compose down -v
docker-compose up -d
```

#### 3. Kafka Issues

```powershell
# Check Kafka logs
docker-compose logs kafka

# List topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Create missing topics manually
docker-compose exec kafka kafka-topics --create --topic message-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

#### 4. WebSocket Connection Failures

- Check message-service logs: `docker-compose logs message-service`
- Verify CORS configuration in API Gateway
- Ensure WebSocket port 8084 is accessible
- Check Nginx proxy configuration for `/ws` path

#### 5. Eureka Registration Issues

```powershell
# Check Eureka dashboard
curl http://localhost:8761

# Verify service logs
docker-compose logs user-service
docker-compose logs message-service

# Restart services
docker-compose restart user-service message-service
```

#### 6. Frontend Not Loading

```powershell
# Check nginx logs
docker-compose logs frontend

# Verify build completed successfully
docker-compose build frontend

# Check nginx configuration
docker-compose exec frontend cat /etc/nginx/conf.d/default.conf
```

#### 7. Out of Memory

```powershell
# Check container resource usage
docker stats

# Reduce JVM memory in Dockerfiles or add to docker-compose.yml:
environment:
  - JAVA_OPTS=-Xmx256m -Xms128m
```

### Debugging Commands

```powershell
# Execute bash in container
docker-compose exec <service-name> sh

# View real-time logs
docker-compose logs -f --tail=100

# Check network connectivity
docker-compose exec user-service ping mysql
docker-compose exec message-service ping kafka

# Inspect volumes
docker volume ls
docker volume inspect basic-chat-app_mysql_data

# Check Docker network
docker network ls
docker network inspect basic-chat-app_chat-network
```

### Complete Reset

If nothing works, perform a complete reset:

```powershell
# Stop and remove everything
docker-compose down -v --remove-orphans

# Remove images (optional)
docker-compose down --rmi all

# Rebuild from scratch
docker-compose build --no-cache
docker-compose up -d
```

## Backup & Maintenance

### Database Backup

```powershell
# Backup MySQL database
docker-compose exec mysql mysqldump -uchatuser -pchatpass chatdb > backup_$(date +%Y%m%d).sql

# Restore from backup
docker-compose exec -T mysql mysql -uchatuser -pchatpass chatdb < backup_20240101.sql
```

### Volume Backup

```powershell
# Backup MySQL volume
docker run --rm -v basic-chat-app_mysql_data:/data -v ${PWD}:/backup ubuntu tar czf /backup/mysql_backup.tar.gz /data

# Restore MySQL volume
docker run --rm -v basic-chat-app_mysql_data:/data -v ${PWD}:/backup ubuntu tar xzf /backup/mysql_backup.tar.gz -C /
```

### Kafka Data Backup

```powershell
# Backup Kafka volume
docker run --rm -v basic-chat-app_kafka_data:/data -v ${PWD}:/backup ubuntu tar czf /backup/kafka_backup.tar.gz /data
```

### Update Application

```powershell
# Pull latest changes (if using Git)
git pull

# Rebuild changed services
docker-compose build

# Recreate containers with new images
docker-compose up -d

# Check logs for issues
docker-compose logs -f
```

### Monitoring

```powershell
# View resource usage
docker stats

# View logs with timestamps
docker-compose logs -f -t

# Export logs to file
docker-compose logs > application.log
```

### Scheduled Maintenance

Create a cron job for automated backups (Linux/Mac):

```bash
# Edit crontab
crontab -e

# Add daily backup at 2 AM
0 2 * * * cd ~/chat-app && docker-compose exec -T mysql mysqldump -uchatuser -pchatpass chatdb > ~/backups/chatdb_$(date +\%Y\%m\%d).sql
```

## Production Best Practices

1. **Security**
   - Use strong, random passwords in production `.env`
   - Never commit `.env` to version control
   - Enable HTTPS with SSL certificates (Let's Encrypt)
   - Restrict database access to internal network only
   - Use secrets management (AWS Secrets Manager, HashiCorp Vault)

2. **Performance**
   - Monitor resource usage with `docker stats`
   - Adjust JVM heap sizes based on load
   - Configure connection pools appropriately
   - Enable caching where applicable

3. **Reliability**
   - Set up automated backups
   - Configure log rotation
   - Monitor disk space
   - Use health checks for all services
   - Set up alerts for service failures

4. **Monitoring**
   - Integrate with ELK stack for log aggregation
   - Use Prometheus + Grafana for metrics
   - Set up APM (Application Performance Monitoring)
   - Configure uptime monitoring

5. **Updates**
   - Test updates in staging environment first
   - Perform rolling updates for zero-downtime
   - Keep Docker images up to date
   - Review security advisories regularly

## Support and Resources

- **Project Repository**: [Add your repo URL]
- **Docker Documentation**: https://docs.docker.com/
- **Docker Compose Reference**: https://docs.docker.com/compose/
- **Spring Boot Docker Guide**: https://spring.io/guides/gs/spring-boot-docker/
- **AWS Lightsail Documentation**: https://aws.amazon.com/lightsail/docs/

## License

[Add your license information]

---

**Last Updated**: 2024
**Version**: 1.0
