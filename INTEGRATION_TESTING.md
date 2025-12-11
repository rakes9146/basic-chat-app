# Integration Testing Guide - Chat Application

## Architecture Overview
```
Angular (4200) → API Gateway (8082) → Backend Services
                                    ├→ USER-SERVICE (Eureka)
                                    └→ MESSAGE-SERVICE (Eureka)
                                                    ├→ PostgreSQL
                                                    ├→ Redis
                                                    └→ Kafka
```

## Step-by-Step Testing

### 1. Start Infrastructure Services
```powershell
# Start Redis
docker run -d -p 6379:6379 --name redis-chat redis:latest

# Start Kafka (if not running)
# Start PostgreSQL (if not running)
```

### 2. Start Backend Services (in order)
```powershell
# Terminal 1: Eureka Server
cd "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\eurekaserver"
.\mvnw.cmd spring-boot:run

# Terminal 2: User Service
cd "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\user-service"
.\mvnw.cmd spring-boot:run

# Terminal 3: Message Service
cd "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\message-service"
.\mvnw.cmd spring-boot:run

# Terminal 4: API Gateway
cd "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\apigateway"
.\mvnw.cmd spring-boot:run
```

### 3. Verify Services are Registered
Open http://localhost:8123 and verify USER-SERVICE and MESSAGE-SERVICE are UP

### 4. Test REST Endpoints via Gateway

#### Test 1: User Login
```powershell
curl.exe -X POST "http://localhost:8082/USER-SERVICE/user/login" `
  -H "Content-Type: application/json" `
  -d '{"userName":"testuser","password":"test123"}'
```
Expected: 200 OK with `true` or 401 Unauthorized

#### Test 2: Get All Users
```powershell
curl.exe "http://localhost:8082/USER-SERVICE/user"
```
Expected: 200 OK with array of users

#### Test 3: Send Message (HTTP)
```powershell
curl.exe -X POST "http://localhost:8082/MESSAGE-SERVICE/message" `
  -H "Content-Type: application/json" `
  -d '{"senderId":1,"receiverId":2,"messageText":"Test message"}'
```
Expected: 201 Created with messageId

#### Test 4: Get Messages
```powershell
curl.exe "http://localhost:8082/MESSAGE-SERVICE/message?senderId=1&receiverId=2"
```
Expected: 200 OK with array of messages

### 5. Test WebSocket via Gateway

#### Test SockJS Info Endpoint
```powershell
curl.exe -i "http://localhost:8082/MESSAGE-SERVICE/ws/info?t=1" `
  -H "Origin: http://localhost:4200"
```
Expected: 200 OK with JSON containing `{"websocket":true}`
Check headers: Should have `Access-Control-Allow-Origin: *` (no duplicates)

### 6. Start Frontend
```powershell
cd "c:\Rakesh New\spring learning\Mentoring Course\Chat Application\chat project\basic-chat-app\frontend"
npm start
```
Open http://localhost:4200

### 7. Test Complete Flow

1. **Login** with two different users in two browsers/incognito windows
   - User A: testuser1
   - User B: testuser2

2. **Check Browser Console** for both users
   - Should see: `[WEBSOCKET] ✅ Connected successfully`
   - Should see: `Subscribed to messages at: /user/{userId}/queue/messages`

3. **User A sends message to User B**
   - Type message and click send
   - User A should see message on RIGHT (green bubble)
   - User B should see message on LEFT (white bubble) instantly

4. **Check message status (ticks)**
   - User A's message should show:
     - Single gray tick (sent)
     - Double gray ticks (delivered) when User B is online
     - Double blue ticks (read) when User B opens the conversation

### 8. Check Backend Logs

#### Message Service Logs to Look For:
```
[SERVICE] Saving message - SenderId: 1, ReceiverId: 2
[SERVICE] ✅ Published MessageSentEvent to Kafka
[KAFKA-CONSUMER] 📨 Received MessageSentEvent
[WEBSOCKET-PUSH] ✅ Message pushed to receiver: 2
[DELIVERY] ✅ Receiver online. Published delivery
```

#### API Gateway Logs:
```
DEBUG - Mapped to lb://MESSAGE-SERVICE
DEBUG - Response status: 200
```

## Common Issues & Fixes

### Issue 1: CORS Errors
**Symptom:** `Access-Control-Allow-Origin header contains multiple values`
**Fix:** Ensure only Gateway has CORS config, backend services allow all origins

### Issue 2: 403 Forbidden on /ws/info
**Symptom:** WebSocket connection fails with 403
**Fix:** Check backend WebSocket config has `setAllowedOriginPatterns("*")`

### Issue 3: Receiver not getting messages
**Symptom:** Sender sees message, receiver doesn't
**Fix:** 
- Check Kafka is running
- Verify `MessageEventPublisher.publishMessageSent()` is called
- Check receiver's WebSocket subscription in browser console

### Issue 4: Old messages not loading
**Symptom:** Database has messages but UI shows empty
**Fix:** 
- Verify `findConversationBetweenUsers()` query is bidirectional
- Check browser console for HTTP GET errors

## Configuration Summary

### Gateway (application.properties)
- Port: 8082
- CORS: allowedOriginPatterns=* (for dev)
- Routes: /USER-SERVICE/**, /MESSAGE-SERVICE/**

### Message Service
- WebSocket endpoint: /ws (with SockJS)
- STOMP prefixes: /app (send), /user (subscribe)
- Brokers: /topic, /queue

### Frontend (environment.ts)
- userServiceUrl: http://localhost:8082/USER-SERVICE/user
- messageServiceUrl: http://localhost:8082/MESSAGE-SERVICE/message
- webSocketUrl: http://localhost:8082/MESSAGE-SERVICE/ws

## Next Steps After Integration Works

1. Add proper authentication/authorization
2. Restrict CORS to specific origins
3. Add SSL/TLS
4. Add error handling and retry logic
5. Add message persistence confirmation
6. Add typing indicators
7. Add file attachments
