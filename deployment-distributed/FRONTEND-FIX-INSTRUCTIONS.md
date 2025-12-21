# ===================================================================
# FRONTEND DEPLOYMENT FIX - Manual Steps
# ===================================================================

## Problem
The nginx configuration inside the Docker image uses Docker service names 
(api-gateway, message-service) which don't exist when services are on 
different AWS instances.

## Solution
Use AWS-specific nginx configuration with actual IP addresses.

---

## Quick Fix Steps

### Step 1: Upload Files to AWS

```powershell
cd "C:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\deployment-distributed"

# Set your SSH key path
$KEY = "C:\path\to\your-key.pem"

# Upload corrected docker-compose.yml
scp -i $KEY instance-6-frontend\docker-compose.yml ec2-user@54.154.129.84:~/docker-compose.yml

# Upload AWS-specific nginx configuration
scp -i $KEY instance-6-frontend\nginx-aws.conf ec2-user@54.154.129.84:~/nginx-aws.conf
```

### Step 2: Update Firewall Rules

**Instance 2 (Services - 35.153.96.103)** needs to allow WebSocket connections:

Go to AWS Lightsail Console → Instance 2 → Networking Tab → Add Rule:
```
Application: Custom
Protocol: TCP
Port: 8083
Source: 54.154.129.84/32  (Frontend Instance)
```

This allows the frontend's nginx to proxy WebSocket connections to the message service.

### Step 3: Deploy on AWS

```powershell
# SSH to Instance 6
ssh -i $KEY ec2-user@54.154.129.84
```

On the AWS instance:
```bash
# Stop existing container
docker-compose down

# Pull latest image
docker-compose pull

# Start with new configuration
docker-compose up -d

# Check logs
docker-compose logs -f frontend
```

You should see:
```
Configuration complete; ready for start up
```

WITHOUT the error:
```
host not found in upstream "api-gateway"
```

### Step 4: Test Frontend

Open browser: **http://54.154.129.84**

Expected: Angular app loads successfully

---

## What Changed?

### Before (nginx.conf in Docker image):
```nginx
location /api/ {
    proxy_pass http://gateway:8082/;  # ❌ Docker service name
}

location /ws/ {
    proxy_pass http://message-service:8083/ws/;  # ❌ Docker service name
}
```

### After (nginx-aws.conf mounted):
```nginx
location /api/ {
    proxy_pass http://54.217.247.163:8082/;  # ✅ Actual AWS IP
}

location /USER-SERVICE/ {
    proxy_pass http://54.217.247.163:8082/USER-SERVICE/;  # ✅ Through Gateway
}

location /MESSAGE-SERVICE/ {
    proxy_pass http://54.217.247.163:8082/MESSAGE-SERVICE/;  # ✅ Through Gateway
}

location /ws/ {
    proxy_pass http://35.153.96.103:8083/ws/;  # ✅ Direct to Message Service
}
```

---

## Verification

1. **Container Running**:
   ```bash
   docker ps | grep chat-frontend
   ```

2. **No Errors in Logs**:
   ```bash
   docker-compose logs frontend | grep -i error
   ```

3. **Nginx Configuration Loaded**:
   ```bash
   docker exec chat-frontend cat /etc/nginx/conf.d/default.conf | grep "54.217"
   ```
   Should show the AWS IP addresses.

4. **Frontend Accessible**:
   ```bash
   curl -I http://54.154.129.84
   ```
   Should return: `HTTP/1.1 200 OK`

5. **Browser Test**:
   - Open: http://54.154.129.84
   - Should see login/register page
   - No console errors

---

## Troubleshooting

### Still getting "refused to connect"?

1. **Check container is running**:
   ```bash
   docker ps
   ```

2. **Check nginx is starting**:
   ```bash
   docker-compose logs frontend | tail -20
   ```

3. **Verify nginx config is mounted**:
   ```bash
   docker exec chat-frontend ls -la /etc/nginx/conf.d/
   ```
   Should show `default.conf` file.

4. **Test nginx config syntax**:
   ```bash
   docker exec chat-frontend nginx -t
   ```

5. **Restart container**:
   ```bash
   docker-compose restart frontend
   ```

### WebSocket not connecting?

1. **Check firewall on Instance 2**:
   - Port 8083 must allow traffic from 54.154.129.84

2. **Test WebSocket endpoint directly**:
   ```bash
   curl -I http://35.153.96.103:8083/ws
   ```

3. **Check browser console** for WebSocket errors

---

## Alternative: Rebuild Frontend Image for AWS

If you want to avoid mounting external config files, rebuild the frontend 
Docker image with AWS IPs baked in:

```powershell
# Copy AWS nginx config to frontend folder
Copy-Item instance-6-frontend\nginx-aws.conf frontend\nginx.conf -Force

# Rebuild frontend image
cd frontend
docker build -t rakes9146/chat-frontend:aws .

# Push to Docker Hub
docker push rakes9146/chat-frontend:aws

# Update docker-compose.yml on AWS to use :aws tag instead of :distributed
```

Then remove the volume mount from docker-compose.yml.

---

## Summary

✅ Created `nginx-aws.conf` with actual AWS IP addresses
✅ Updated `docker-compose.yml` to mount AWS-specific config
✅ Documented firewall rule needed for WebSocket
✅ Ready to deploy!
