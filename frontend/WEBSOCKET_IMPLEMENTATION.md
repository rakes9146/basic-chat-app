# WebSocket Implementation Guide

## Overview

The chat application now supports **real-time messaging** using WebSocket with STOMP (Simple Text Oriented Messaging Protocol) and SockJS fallback. This replaces HTTP polling with true bidirectional communication.

## Architecture

### Backend (Spring Boot - Message Service)
```
┌─────────────────────────────────────────────────────────────┐
│                   Message Service                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  WebSocketStompConfig (/ws - STOMP Endpoint)                │
│    ├── /app/send (MessageMapping)                           │
│    ├── /app/deliver (MessageMapping)                        │
│    └── /app/read (MessageMapping)                           │
│                                                              │
│  WebSocketMessageController                                 │
│    └── Handles message, delivery, and read events           │
│                                                              │
│  Kafka Topics                                               │
│    ├── chat.message.sent                                    │
│    ├── chat.message.delivered                               │
│    └── chat.message.read                                    │
│                                                              │
│  WebSocketPushConsumer                                      │
│    ├── Listens to Kafka topics                             │
│    ├── Pushes to /user/{userId}/queue/messages             │
│    └── Pushes to /user/{userId}/queue/status               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Frontend (Angular)

```
┌─────────────────────────────────────────────────────────────┐
│                   Chat Application                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  WebsocketService (websocket.service.ts)                    │
│    ├── connect()                                            │
│    ├── disconnect()                                         │
│    ├── sendMessage()                                        │
│    ├── markMessageDelivered()                               │
│    ├── markMessageRead()                                    │
│    ├── subscribeToMessages()                                │
│    └── subscribeToDeliveryStatus()                          │
│                                                              │
│  ChatComponent                                              │
│    ├── Connects to WebSocket on init                        │
│    ├── Subscribes to incoming messages                      │
│    ├── Listens for delivery/read status                     │
│    ├── Sends messages via WebSocket                         │
│    └── Falls back to HTTP if disconnected                   │
│                                                              │
│  STOMP/SockJS Client Library                                │
│    └── ws://localhost:8080/ws                              │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Installation

### 1. Add Dependencies to package.json

```json
{
  "dependencies": {
    "@stomp/stompjs": "^7.0.0",
    "sockjs-client": "^1.6.1"
  }
}
```

### 2. Install Dependencies

```bash
npm install
```

## WebSocket Service (websocket.service.ts)

### Configuration

```typescript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

private client: Client;
private wsUrl = 'ws://localhost:8080/ws';

this.client = new Client({
  brokerURL: this.wsUrl,
  onConnect: () => this.onConnect(),
  onDisconnect: () => this.onDisconnect(),
  onStompError: (frame: IFrame) => this.onStompError(frame),
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
});
```

### Key Methods

#### Connect to WebSocket
```typescript
websocketService.connect();
```

#### Send Message
```typescript
const message: WebSocketMessage = {
  messageText: 'Hello',
  senderId: 1,
  receiverId: 2,
  isDelivered: false,
  isRead: false,
  timestamp: new Date()
};

websocketService.sendMessage(message);
// Publishes to: /app/send
```

#### Mark Message Delivered
```typescript
websocketService.markMessageDelivered(messageId, receiverId);
// Publishes to: /app/deliver
```

#### Mark Message Read
```typescript
websocketService.markMessageRead(messageId, receiverId);
// Publishes to: /app/read
```

#### Subscribe to Incoming Messages
```typescript
const subscription = websocketService.subscribeToMessages(userId);

websocketService.messages$.subscribe((message: WebSocketMessage) => {
  console.log('Received:', message);
});
```

#### Subscribe to Status Updates
```typescript
const subscription = websocketService.subscribeToDeliveryStatus(userId);

websocketService.deliveryStatus$.subscribe((status: MessageStatus) => {
  console.log('Delivered:', status);
});

websocketService.readStatus$.subscribe((status: MessageStatus) => {
  console.log('Read:', status);
});
```

## Message Flow

### Sending a Message

```
1. User types message and clicks Send
                ↓
2. ChatComponent.sendMessage()
                ↓
3. WebsocketService.sendMessage(messageDto)
                ↓
4. STOMP publishes to /app/send
                ↓
5. WebSocketMessageController.send()
                ↓
6. MessageService.saveNewMessage() - saves to DB
                ↓
7. Kafka event: MessageSentEvent
                ↓
8. WebSocketPushConsumer.pushMessage()
                ↓
9. SimpMessagingTemplate.convertAndSendToUser()
                ↓
10. Receiver's /user/{receiverId}/queue/messages
                ↓
11. Backend publishes MessageDeliveredEvent to Kafka
                ↓
12. WebSocketPushConsumer.notifySenderDelivered()
                ↓
13. Sender's /user/{senderId}/queue/status
                ↓
14. ChatComponent receives status update → UI updates (✓✓)
```

### Receiving a Message

```
1. Backend publishes MessageSentEvent to Kafka
                ↓
2. WebSocketPushConsumer listens to chat.message.sent topic
                ↓
3. Converts to SimpMessaging format
                ↓
4. Sends to /user/{receiverId}/queue/messages
                ↓
5. Angular WebsocketService.subscribeToMessages()
                ↓
6. WebsocketService.messages$ Subject emits
                ↓
7. ChatComponent receives in subscription
                ↓
8. Adds to this.messages[] array
                ↓
9. Template updates automatically (change detection)
                ↓
10. User sees new message in chat UI
                ↓
11. ChatComponent calls websocketService.markMessageRead()
                ↓
12. Backend publishes MessageReadEvent
                ↓
13. Sender notified with status (✓✓ blue)
```

## Subscription Management

### In ChatComponent ngOnInit:

```typescript
ngOnInit(): void {
  // Connect to WebSocket
  this.websocketService.connect();
  
  // Subscribe to connection status
  this.websocketService.isConnected$.pipe(takeUntil(this.destroy$))
    .subscribe(isConnected => {
      this.wsConnected = isConnected;
      if (isConnected && this.currentUser) {
        this.subscribeToWebSocketMessages();
      }
    });

  // Subscribe to current user
  this.authService.currentUser$.pipe(takeUntil(this.destroy$))
    .subscribe(user => {
      this.currentUser = user;
      if (user && this.wsConnected) {
        this.subscribeToWebSocketMessages();
        this.loadAllUsers();
      }
    });
}
```

### In ChatComponent ngOnDestroy:

```typescript
ngOnDestroy(): void {
  // Cleanup subscriptions
  if (this.messageSubscription) {
    this.messageSubscription.unsubscribe();
  }
  if (this.deliveryStatusSubscription) {
    this.deliveryStatusSubscription.unsubscribe();
  }
  if (this.readStatusSubscription) {
    this.readStatusSubscription.unsubscribe();
  }
  
  this.websocketService.disconnect();
  this.destroy$.next();
  this.destroy$.complete();
}
```

## Protocol Details

### STOMP Frame Format

#### Message Send
```
SEND
destination:/app/send
content-type:application/json

{
  "messageText":"Hello",
  "senderId":1,
  "receiverId":2,
  "isDelivered":false,
  "isRead":false,
  "timestamp":"2025-01-15T10:30:00Z"
}
```

#### Subscribe to Messages
```
SUBSCRIBE
id:1
destination:/user/1/queue/messages

// Receives frames like:
MESSAGE
destination:/user/1/queue/messages
message-id:..

{
  "messageId":123,
  "messageText":"Hello",
  "senderId":2,
  "receiverId":1,
  "isDelivered":true,
  "isRead":false,
  "timestamp":"2025-01-15T10:30:00Z"
}
```

## Error Handling

### Connection Errors

```typescript
websocketService.connectionError$.subscribe(error => {
  console.error('WebSocket error:', error);
  // Implement retry logic or fallback
});
```

### Fallback to HTTP

If WebSocket connection fails, the application automatically falls back to HTTP:

```typescript
sendMessage(): void {
  if (this.wsConnected) {
    // Use WebSocket (fast, real-time)
    this.websocketService.sendMessage(wsMessage);
  } else {
    // Fallback to HTTP
    this.messageService.sendMessage(messageDto).subscribe(...);
  }
}
```

## Connection Lifecycle

```
CONNECT
  ↓
STOMP CONNECTED (handshake)
  ↓
SUBSCRIBE /user/{userId}/queue/messages
  ↓
SUBSCRIBE /user/{userId}/queue/status
  ↓
Ready for messaging
  ↓
DISCONNECT
  ↓
Connection closed
```

## Performance Benefits

| Feature | HTTP Polling | WebSocket |
|---------|---|---|
| Message Delivery | ~1-5 seconds delay | Real-time (<100ms) |
| Server Load | High (repeated requests) | Low (persistent connection) |
| Bandwidth | Wasteful (many headers) | Efficient (binary frames) |
| User Experience | Delayed updates | Instant updates |
| Scalability | Poor | Excellent |

## Troubleshooting

### 1. WebSocket Connection Fails
```
Check: 
- Backend is running on port 8080
- API Gateway is routing /ws to message-service
- Firewall allows WebSocket protocol
- Browser console shows connection attempts
```

### 2. Messages Not Received
```
Check:
- User is subscribed to /user/{userId}/queue/messages
- userId is correctly set in User model
- Kafka is running and message-sent topic has consumers
- Check browser WebSocket tab in DevTools
```

### 3. Slow Message Delivery
```
Check:
- Kafka broker is responsive
- Database connections are not exhausted
- Network latency between services
- Message-service logs for errors
```

### 4. Connection Drops Frequently
```
Check:
- Proxy/load balancer WebSocket support
- SSL/TLS certificate issues (for wss://)
- Network stability
- Increase reconnectDelay in WebsocketService if too aggressive
```

## Environment Configuration

### development (environment.ts)
```typescript
export const environment = {
  production: false,
  webSocketUrl: 'ws://localhost:8080/ws'
};
```

### production (environment.prod.ts)
```typescript
export const environment = {
  production: true,
  webSocketUrl: 'wss://yourdomain.com/ws' // Use wss:// for HTTPS
};
```

## Browser Support

✅ Chrome 43+
✅ Firefox 49+
✅ Safari 10.1+
✅ Edge 14+
✅ IE 10+ (via SockJS fallback)

## Security Considerations

1. **CORS**: Backend must allow origin
2. **Authentication**: Add Bearer token to WebSocket handshake if needed
3. **Authorization**: Verify user can access /user/{userId}/queue/*
4. **Message Validation**: Validate sender/receiver IDs match authenticated user
5. **Rate Limiting**: Implement message rate limiting on backend

## Future Enhancements

- [ ] Message read receipts animation
- [ ] Typing indicators via WebSocket
- [ ] User online/offline status
- [ ] Message search optimization with Elasticsearch
- [ ] Video call signaling via WebSocket
- [ ] File transfer progress tracking
- [ ] Encryption for sensitive messages
