# Chat Application - Quick Start Guide

## 🚀 Quick Local Deployment

### Prerequisites
- Docker Desktop installed and running
- At least 4GB RAM available

### Steps

1. **Create environment file**
   ```powershell
   Copy-Item .env.example .env
   ```

2. **Start all services**
   ```powershell
   docker-compose up -d
   ```

3. **Wait for services to be ready** (2-3 minutes)
   ```powershell
   docker-compose ps
   ```

4. **Access the application**
   - Frontend: http://localhost
   - Eureka Dashboard: http://localhost:8761
   - API Gateway: http://localhost:8082

### Stop Services
```powershell
docker-compose down
```

### View Logs
```powershell
docker-compose logs -f
```

---

## 📋 What Gets Deployed

1. **MySQL** - Database (port 3306)
2. **Kafka** - Message broker (port 9092)  
3. **Redis** - Cache (port 6379)
4. **Eureka Server** - Service discovery (port 8761)
5. **API Gateway** - Routing (port 8082)
6. **User Service** - User management
7. **Message Service** - Real-time chat with WebSocket
8. **Frontend** - Angular UI served by Nginx (port 80)

---

## 🔍 Verify Deployment

```powershell
# Check all services are running
docker-compose ps

# All should show "Up" or "Up (healthy)"
```

Expected output:
```
NAME                  STATUS
mysql                 Up (healthy)
kafka                 Up (healthy)
redis                 Up
eureka                Up
api-gateway           Up
user-service          Up
message-service       Up
frontend              Up
```

---

## ⚠️ Troubleshooting

**Services not starting?**
```powershell
docker-compose logs <service-name>
```

**Complete reset:**
```powershell
docker-compose down -v
docker-compose up -d
```

**For detailed deployment guide, see [DEPLOYMENT.md](./DEPLOYMENT.md)**

---

## 🌐 Production Deployment (AWS Lightsail)

See full instructions in [DEPLOYMENT.md](./DEPLOYMENT.md#aws-lightsail-deployment)

Quick overview:
1. Create Ubuntu 22.04 instance (4GB+ RAM)
2. Install Docker & Docker Compose
3. Upload project files
4. Configure production `.env`
5. Update frontend environment with server IP
6. Run `docker-compose up -d`

---

## 📦 Data Persistence

Data is stored in Docker volumes:
- `mysql_data` - All user data and messages
- `kafka_data` - Message queue data

Volumes persist even after `docker-compose down`.

To completely remove data:
```powershell
docker-compose down -v  # -v flag removes volumes
```

---

## 🔄 Update Application

```powershell
# Pull latest changes
git pull

# Rebuild and restart
docker-compose up -d --build
```

---

## 📞 Need Help?

- Check logs: `docker-compose logs -f`
- Full documentation: [DEPLOYMENT.md](./DEPLOYMENT.md)
- Troubleshooting section: [DEPLOYMENT.md#troubleshooting](./DEPLOYMENT.md#troubleshooting)
