# Frontend API Configuration Fix

## Problem
Angular frontend was calling `http://localhost:8082` for API requests, which works in local development but fails when deployed to AWS because:
1. The browser (client-side) tries to connect to localhost
2. Localhost in the browser ≠ AWS Gateway at 54.217.247.163:8082

## Solution Applied
Changed Angular to use **relative URLs** and let nginx proxy the requests to the actual AWS services.

---

## What Was Changed

### 1. Angular Environment Files

**Before**:
```typescript
// environment.ts & environment.prod.ts
userServiceUrl: 'http://localhost:8082/USER-SERVICE/user',
messageServiceUrl: 'http://localhost:8082/MESSAGE-SERVICE/message',
webSocketUrl: 'http://localhost:8082/MESSAGE-SERVICE/ws'
```

**After**:
```typescript
// environment.ts & environment.prod.ts
userServiceUrl: '/USER-SERVICE/user',
messageServiceUrl: '/MESSAGE-SERVICE/message',
webSocketUrl: '/ws'
```

### 2. Request Flow

**Old Flow** (❌ Failed):
```
Browser → http://localhost:8082/USER-SERVICE/... → 404 Not Found
```

**New Flow** (✅ Works):
```
Browser → http://54.154.129.84/USER-SERVICE/... 
        → Nginx (on Instance 6) 
        → http://54.217.247.163:8082/USER-SERVICE/... (API Gateway on Instance 1)
        → Service on Instance 2
```

---

## Deployment Steps

### Step 1: Rebuild Frontend Image

```powershell
cd "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\deployment-distributed"

# Run the rebuild script
.\rebuild-frontend.ps1
```

Or manually:

```powershell
cd ..\frontend

# Build new image with updated environment files
docker build -t rakes9146/chat-frontend:distributed .

# Push to Docker Hub
docker push rakes9146/chat-frontend:distributed
```

### Step 2: Update on AWS Instance 6

```powershell
# SSH to Instance 6
$KEY = "C:\path\to\your-key.pem"
ssh -i $KEY ec2-user@54.154.129.84
```

On AWS:
```bash
# Stop current container
docker-compose down

# Pull new image
docker-compose pull

# Start with new image
docker-compose up -d

# Watch logs
docker-compose logs -f frontend
```

### Step 3: Verify Configuration

On AWS Instance 6:
```bash
# Check if nginx config is loaded
docker exec chat-frontend cat /etc/nginx/conf.d/default.conf | grep "54.217"

# Should show:
# proxy_pass http://54.217.247.163:8082/USER-SERVICE/;
# proxy_pass http://54.217.247.163:8082/MESSAGE-SERVICE/;
# proxy_pass http://35.153.96.103:8083/ws/;
```

---

## Testing

### 1. Open Frontend
```
http://54.154.129.84
```

### 2. Test User Registration
1. Click "Register"
2. Fill in:
   - Username: `testuser`
   - Email: `test@example.com`
   - Password: `Test@123`
3. Click Submit

### 3. Check Browser Network Tab

**Expected Requests**:
```
POST http://54.154.129.84/USER-SERVICE/user/register
→ Status: 200 OK or 201 Created
```

**NOT**:
```
POST http://localhost:8082/USER-SERVICE/user/register
→ Status: Failed (net::ERR_CONNECTION_REFUSED)
```

### 4. Check Browser Console

Should see NO errors like:
- ❌ `net::ERR_CONNECTION_REFUSED`
- ❌ `Failed to fetch`
- ❌ `CORS error`

---

## Nginx Proxy Routes

The nginx-aws.conf has these routes configured:

```nginx
# User Service API
location /USER-SERVICE/ {
    proxy_pass http://54.217.247.163:8082/USER-SERVICE/;
}

# Message Service API
location /MESSAGE-SERVICE/ {
    proxy_pass http://54.217.247.163:8082/MESSAGE-SERVICE/;
}

# WebSocket
location /ws/ {
    proxy_pass http://35.153.96.103:8083/ws/;
}
```

---

## Troubleshooting

### Issue: Still calling localhost

**Check 1**: Verify new image was pulled
```bash
docker images | grep chat-frontend
# Check the IMAGE ID changed after pull
```

**Check 2**: Verify container is using new image
```bash
docker-compose down
docker-compose pull
docker-compose up -d --force-recreate
```

**Check 3**: Clear browser cache
- Hard refresh: Ctrl+Shift+R (Windows/Linux) or Cmd+Shift+R (Mac)
- Or open in incognito/private window

### Issue: 404 Not Found on API calls

**Check 1**: Verify nginx config is mounted
```bash
docker exec chat-frontend ls -la /etc/nginx/conf.d/
# Should show default.conf
```

**Check 2**: Test nginx proxy manually
```bash
# From AWS Instance 6
curl -I http://localhost/USER-SERVICE/actuator/health
# Should return 200 or 401, NOT 404
```

**Check 3**: Verify Gateway is accessible
```bash
curl http://54.217.247.163:8082/actuator/health
# Should return 200 OK
```

### Issue: CORS errors

**Check**: Gateway CORS configuration
Gateway should allow requests from frontend origin (54.154.129.84).

---

## Summary of Fixes

✅ Changed Angular environment files to use relative URLs
✅ Nginx proxies `/USER-SERVICE/` to Gateway at 54.217.247.163:8082
✅ Nginx proxies `/MESSAGE-SERVICE/` to Gateway at 54.217.247.163:8082
✅ Nginx proxies `/ws/` to Message Service at 35.153.96.103:8083
✅ Browser makes requests to its own domain (54.154.129.84)
✅ No hardcoded localhost or IP addresses in Angular code

This approach makes the frontend portable - it works regardless of the actual backend IPs because nginx handles the routing!
