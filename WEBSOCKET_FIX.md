# WebSocket Issues - Fix Summary

## Issues Fixed

### 1. CORS Error for WebSocket
**Problem**: CORS error when trying to connect to `http://localhost:8082/MESSAGE-SERVICE/ws/info?t=...`

**Root Cause**: 
- WebSocket STOMP configuration didn't have proper CORS setup
- API Gateway needed explicit WebSocket route configuration
- Missing CORS filter for WebSocket upgrade headers

**Solutions Applied**:

#### A. Updated WebSocketStompConfig.java
- Added both SockJS and native WebSocket endpoints
- Configured `setAllowedOriginPatterns("*")` for both

#### B. Created CorsConfig.java
- Added global CORS filter with WebSocket headers
- Included `Upgrade`, `Connection`, `Sec-WebSocket-*` headers
- Enabled credentials support

#### C. Updated API Gateway configuration
- Changed from `server.webflux.discovery` to `discovery.locator.enabled`
- Added explicit routes for WebSocket (`lb:ws://MESSAGE-SERVICE`)
- Added explicit routes for HTTP (`lb://MESSAGE-SERVICE`)
- Added `allowCredentials=true` to CORS configuration

---

### 2. WebSocket Not Being Called After Sending Message
**Problem**: After sending message, only MessageController was being called, not WebSocket

**Analysis**: 
This is **NOT A BUG** - it's the **correct designed flow**:

1. ✅ Frontend calls HTTP POST `/message` (MessageController)
2. ✅ Backend saves message to database
3. ✅ Backend returns response with `messageId`
4. ✅ Frontend receives `messageId`
5. ✅ Frontend broadcasts via WebSocket `/app/send` with `messageId`
6. ✅ WebSocketMessageController receives it
7. ✅ Backend publishes Kafka event
8. ✅ WebSocketPushConsumer pushes to receiver

**Why This Design?**
- HTTP ensures message is persisted (reliable)
- WebSocket broadcasts it for real-time delivery (fast)
- messageId from HTTP links both operations
- If WebSocket fails, message is still saved

**Verification**:
Check logs for this sequence:
```
[CONTROLLER] Message created successfully - ID: 123
[WEBSOCKET] /send received: messageId=123
[KAFKA] Publishing MessageSentEvent
[WEBSOCKET-PUSH] Message pushed to receiver
```

---

## Files Changed

### Backend Changes:

1. **WebSocketStompConfig.java** (Updated)
   - Added native WebSocket endpoint (non-SockJS)
   - Both endpoints use `setAllowedOriginPatterns("*")`

2. **CorsConfig.java** (New File)
   - Global CORS filter for WebSocket
   - Exposes WebSocket upgrade headers
   - Allows credentials

### Gateway Changes:

3. **application.properties** (Updated)
   - Explicit WebSocket route: `lb:ws://MESSAGE-SERVICE`
   - Explicit HTTP route: `lb://MESSAGE-SERVICE`
   - Added `allowCredentials=true` to CORS
   - Fixed discovery locator configuration

---

## Testing Steps

### 1. Verify Backend is Running
```bash
# Check if message-service is registered with Eureka
curl http://localhost:8123/eureka/apps/MESSAGE-SERVICE
```

### 2. Test WebSocket Connection
Open browser console when frontend loads:
```
Expected logs:
[WEBSOCKET] Connecting to WebSocket: http://localhost:8082/MESSAGE-SERVICE/ws
[WEBSOCKET] ✅ Connected successfully to: http://localhost:8082/MESSAGE-SERVICE/ws
[WEBSOCKET] 🟢 Presence announced: User X is online
```

### 3. Test Message Flow
Send a message and check console:
```
Expected frontend logs:
[HTTP] 📤 Saving message: {...}
[HTTP] 📥 Response: {messageId: 123}
[WEBSOCKET] 📡 Broadcasted with messageId: 123

Expected backend logs:
[CONTROLLER] Message created successfully - ID: 123
[WEBSOCKET] /send received: messageId=123
[KAFKA] Publishing MessageSentEvent: {...}
[WEBSOCKET-PUSH] ✅ Message pushed to receiver: 2
```

### 4. Verify CORS Headers
Check browser network tab for `/ws/info`:
```
Response Headers should include:
Access-Control-Allow-Origin: *
Access-Control-Allow-Credentials: true
Access-Control-Allow-Headers: *
```

---

## Common Issues & Solutions

### Issue: Still Getting CORS Error
**Solution**: 
1. Clear browser cache
2. Restart API Gateway: `mvn spring-boot:run`
3. Restart message-service
4. Hard refresh browser (Ctrl+Shift+R)

### Issue: WebSocket Connects but Doesn't Send Messages
**Check**:
```javascript
// In browser console:
websocketService.isConnected()  // Should return true
```

**Solution**: 
- Verify presence was announced (check logs for "🟢 Presence announced")
- Check if chat activity was set (check logs for "💬 Set chat active")

### Issue: Message Sent but Receiver Doesn't Get It
**Check Backend Logs**:
```
[KAFKA-CONSUMER] 📨 Received MessageSentEvent
[WEBSOCKET-PUSH] ✅ Message pushed to receiver
```

**Solution**:
- Verify Kafka is running: `docker ps | grep kafka`
- Verify Redis is running: `docker ps | grep redis`
- Check receiver is online: Look for receiver's presence announcement

### Issue: API Gateway Not Forwarding to message-service
**Check Eureka Dashboard**: `http://localhost:8123`
- Verify MESSAGE-SERVICE is registered
- Verify API-GATEWAY is registered

**Check Gateway Logs**:
```
Pattern: /MESSAGE-SERVICE/ws/**
Matched: true
Forwarding to: ws://192.168.x.x:xxxxx/ws
```

---

## Architecture Flow

```
Frontend (Angular on localhost:4200)
    ↓
    HTTP POST /message
    ↓
API Gateway (localhost:8082)
    ↓
    Forward to MESSAGE-SERVICE/message
    ↓
MessageController.saveMessage()
    ↓
    Save to MySQL
    ↓
    Return {messageId: 123}
    ↓
    ← Response back to Frontend
    ↓
Frontend receives messageId
    ↓
    WebSocket STOMP /app/send {messageId: 123}
    ↓
API Gateway (WebSocket route)
    ↓
    Forward via ws://MESSAGE-SERVICE/ws
    ↓
WebSocketMessageController.send()
    ↓
    Publish to Kafka: chat.message.sent
    ↓
WebSocketPushConsumer.pushMessage()
    ↓
    Check: Receiver online? ✓
    ↓
    Check: Receiver active in chat? ✓
    ↓
    Push to: /user/{receiverId}/queue/messages
    ↓
    Auto-publish delivery ✓
    ↓
    Auto-publish read (if active) ✓
    ↓
Receiver's Frontend
    ↓
    Receives message via WebSocket subscription
    ↓
    Displays in UI + Sends receipts
```

---

## Configuration Summary

### API Gateway (port 8082)
- WebSocket Route: `/MESSAGE-SERVICE/ws/**` → `lb:ws://MESSAGE-SERVICE`
- HTTP Route: `/MESSAGE-SERVICE/**` → `lb://MESSAGE-SERVICE`
- CORS: Allow all origins with credentials

### Message Service (dynamic port via Eureka)
- WebSocket Endpoint: `/ws` (with SockJS and native)
- REST Endpoint: `/message`
- STOMP Destinations: `/app/send`, `/app/deliver`, `/app/read`, `/app/presence`, `/app/chat/active`

### Frontend (port 4200)
- WebSocket URL: `http://localhost:8082/MESSAGE-SERVICE/ws`
- HTTP URL: `http://localhost:8082/MESSAGE-SERVICE/message`

---

## Next Steps

1. **Restart Services in Order**:
   ```bash
   # 1. Eureka Server
   cd eureka-server
   mvn spring-boot:run
   
   # 2. API Gateway (wait for Eureka)
   cd api-gateway
   mvn spring-boot:run
   
   # 3. Message Service (wait for Gateway)
   cd message-service
   mvn spring-boot:run
   
   # 4. Frontend
   cd frontend
   ng serve
   ```

2. **Verify All Services**:
   - Eureka: http://localhost:8123
   - Gateway: http://localhost:8082/actuator/health
   - Frontend: http://localhost:4200

3. **Test Complete Flow**:
   - Login with User 1
   - Select conversation
   - Send message
   - Check console for complete flow logs

---

## Success Indicators

✅ No CORS errors in browser console
✅ WebSocket connection established
✅ Presence announced on login
✅ Chat activity set on conversation select
✅ HTTP POST creates message with messageId
✅ WebSocket STOMP sends message with messageId
✅ Kafka events published
✅ Receiver gets message in real-time
✅ Delivery and read receipts work

---

## Rollback (If Issues Persist)

If you encounter issues, rollback is easy:
```bash
git diff HEAD
git checkout -- <file>
```

Files changed:
- message-service/config/WebSocketStompConfig.java
- message-service/config/CorsConfig.java (new)
- apigateway/application.properties
