# 🏠 Local Development Setup (No Docker for Frontend)

## ✅ Architecture for Local Testing

**Backend Services (Docker):**
- MySQL (port 3306)
- Kafka (port 9092)  
- Redis (port 6379)
- Eureka Server (port 8761)
- API Gateway (port 8082)
- User Service (port 8081)
- Message Service (port 8083)

**Frontend (Local - No Docker):**
- Angular Dev Server (port 4200)
- Connects directly to localhost:8082 and localhost:8083

---

## 🚀 Quick Start

### Step 1: Start Backend Services

```powershell
cd "C:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\deployment-distributed"
docker-compose -f docker-compose-local.yml up -d
```

Wait 60-90 seconds for all services to start.

### Step 2: Verify Backend Services

```powershell
# Check all containers running
docker ps

# Test Eureka
Start-Process "http://localhost:8761"

# Test Gateway
curl http://localhost:8082/actuator/health
```

### Step 3: Start Frontend (Separate Terminal)

```powershell
cd "C:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\frontend"
ng serve
```

Or if you want to specify port:
```powershell
ng serve --port 4200
```

### Step 4: Access Application

Open browser: **http://localhost:4200**

---

## 🔍 How It Works

### Frontend Configuration

Your Angular app should have these settings in `environment.ts` or `environment.development.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8082/api',
  websocketUrl: 'ws://localhost:8083/ws'
};
```

### Why No Nginx for Local?

- ✅ Faster development - instant reload with `ng serve`
- ✅ No proxy complexity - direct connections
- ✅ Easier debugging - see network calls directly
- ✅ CORS already handled by Spring Boot services

### Nginx is for AWS Only

Nginx reverse proxy is only needed on AWS to:
- Route `/api/*` → API Gateway
- Route `/ws/*` → Message Service WebSocket
- Serve static frontend files

---

## 📊 Monitoring

### Check Service Status

```powershell
# All containers
docker ps

# Specific logs
docker logs chat-eureka-local -f
docker logs chat-gateway-local -f
docker logs chat-user-service-local -f
docker logs chat-message-service-local -f
```

### Check Eureka Registry

Open: http://localhost:8761

Should show:
- ✅ API-GATEWAY
- ✅ USER-SERVICE  
- ✅ MESSAGE-SERVICE

---

## 🛑 Stop Services

### Stop Backend

```powershell
docker-compose -f docker-compose-local.yml down
```

### Stop Frontend

Press `Ctrl+C` in the terminal running `ng serve`

---

## 🐛 Troubleshooting

### Issue: Frontend Can't Connect to Backend

**Check CORS settings** in your Spring Boot services:

```java
// Gateway or Service
@CrossOrigin(origins = "http://localhost:4200")
```

### Issue: WebSocket Connection Failed

**Check WebSocket configuration**:
1. Message Service is running: `docker ps | findstr message`
2. Port 8083 is accessible: `curl http://localhost:8083/actuator/health`
3. WebSocket URL correct: `ws://localhost:8083/ws`

### Issue: Service Shows as Unhealthy

Wait 30-60 seconds for health checks to pass. If still unhealthy:

```powershell
docker logs [service-name]
docker restart [service-name]
```

---

## 📝 Summary

| Component | Where | Port | Access |
|-----------|-------|------|--------|
| MySQL | Docker | 3306 | Internal |
| Kafka | Docker | 9092 | Internal |
| Redis | Docker | 6379 | Internal |
| Eureka | Docker | 8761 | http://localhost:8761 |
| Gateway | Docker | 8082 | http://localhost:8082 |
| User Service | Docker | 8081 | http://localhost:8081 |
| Message Service | Docker | 8083 | http://localhost:8083 |
| **Frontend** | **Local** | **4200** | **http://localhost:4200** |

---

## 🎯 Development Workflow

```powershell
# 1. Start Docker services
docker-compose -f docker-compose-local.yml up -d

# 2. Wait for services (check Eureka dashboard)
Start-Process "http://localhost:8761"

# 3. Start frontend
cd frontend
ng serve

# 4. Develop! 
# Frontend hot-reloads automatically
# Backend needs rebuild if you change Java code

# 5. Stop everything when done
docker-compose -f docker-compose-local.yml down
# Ctrl+C in ng serve terminal
```

---

**🎉 This is the proper local development setup!**

- Backend runs in Docker (consistent environment)
- Frontend runs locally with `ng serve` (fast development)
- No nginx complexity for local testing
- Save nginx configuration for AWS deployment only
