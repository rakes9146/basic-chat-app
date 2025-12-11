# Message Flow Specifications - Implementation

## Overview
This document describes the complete implementation of the WhatsApp-like message delivery and read receipt system.

## Message Flow

### Scenario 1: A sends message to B (B is online and in chat)
**Flow:**
1. A sends message via WebSocket `/app/send`
2. Backend saves to DB with messageId
3. Backend pushes message to **both A and B** via `/user/{userId}/queue/messages`
4. **A's frontend**: Receives message, adds to chat UI, status = 'sent'
5. **B's frontend**: Receives message, adds to chat UI, immediately calls:
   - `markMessageDelivered()` → sends to `/app/deliver`
   - `markMessageRead()` → sends to `/app/read`
6. Backend publishes to Kafka: `chat.message.delivered` and `chat.message.read`
7. Kafka consumer pushes status to **A** via `/user/{userId}/queue/status`
8. **A's frontend**: Receives status updates, changes tick to blue double tick ✓✓

**Result:** A sees message with blue double tick (read)

---

### Scenario 2: A sends message to B (B is online but NOT in chat)
**Flow:**
1. A sends message → Backend saves → Pushes to both A and B
2. **A's frontend**: Shows message with single tick (sent)
3. **B's frontend**: Receives message but chat not open
   - Calls `markMessageDelivered()` only (NOT read)
4. Backend → Kafka → Status pushed to A
5. **A's frontend**: Updates to double grey tick (delivered)

**Result:** A sees grey double tick (delivered, not read)

---

### Scenario 3: A sends message to B (B is offline)
**Flow:**
1. A sends message → Backend saves → Attempts to push to B
2. Backend detects B not in `SimpUserRegistry` (no WebSocket session)
3. Backend does NOT publish delivery event
4. **A's frontend**: Shows single tick (sent only)

**Result:** A sees single tick (sent, not delivered)

---

### Scenario 4: B comes online later (after A sent message while B offline)
**Flow:**
1. B opens browser → Connects to WebSocket
2. B loads messages via REST API (includes status from DB)
3. B sees unread messages, calls `markMessageDelivered()` for each
4. Backend → Kafka → Status pushed to **A** (if A is online)
5. **A's UI updates**: Single tick → Double grey tick

**Result:** A's status updates when B comes online

---

### Scenario 5: A sends message while B offline, then A goes offline, B comes online
**Flow:**
1. A sends message → Backend saves → A sees single tick → A goes offline
2. B comes online → Loads messages → Marks as delivered
3. Backend tries to push status to A but A is offline
4. Status saved in DB (isDelivered = true)
5. **A comes back online** → Loads messages via REST API
6. A's frontend sees `isDelivered = true` in REST response → Shows double tick

**Result:** Status persisted in DB, A sees updated status when reconnects

---

### Scenario 6: B opens chat with A later (marks as read)
**Flow:**
1. B has received messages (double tick on A's side)
2. B clicks on A's conversation → Switches to chat
3. Frontend calls `markMessageRead()` for each message
4. Backend → Kafka → Status pushed to A
5. **A's UI**: Grey double tick → Blue double tick

**Result:** A sees blue tick when B opens the chat

---

## Implementation Details

### Frontend (chat.component.ts)

#### sendMessage()
```typescript
- Clears input field
- Sends message via WebSocket with timestamp
- Backend handles: save, push to both users, status updates
```

#### Message Reception (messages$ subscriber)
```typescript
- Receives message with messageId from backend
- Determines if user is sender or receiver
- Checks if message already exists (prevent duplicates)
- Adds to UI ONLY if conversation is currently open
- For receiver:
  - If chat open: mark as delivered AND read
  - If chat NOT open: mark as delivered only
- Updates conversation list
```

#### Status Updates (deliveryStatus$ and readStatus$ subscribers)
```typescript
- Receives status updates via /queue/status
- Finds message by ID in current messages array
- Updates message.status to 'delivered' or 'read'
- UI automatically reflects changes (Angular change detection)
```

### Backend (WebSocketMessageController.java)

#### /send endpoint
```java
- Saves message to DB with messageId
- Creates message payload with all fields
- Pushes to receiver: /user/{receiverId}/queue/messages
- Pushes to sender: /user/{senderId}/queue/messages (confirmation)
```

#### /deliver endpoint
```java
- Updates isDelivered = true in DB
- Fetches message to get senderId
- Publishes MessageDeliveredEvent to Kafka
```

#### /read endpoint
```java
- Updates isRead = true in DB
- Fetches message to get senderId  
- Publishes MessageReadEvent to Kafka
```

### Backend (WebSocketPushConsumer.java - Kafka)

#### pushMessage (chat.message.sent)
```java
- Checks if receiver has active WebSocket session
- Pushes message to receiver via /queue/messages
- If receiver online: publishes delivery event
- If receiver in chat with sender: also publishes read event
```

#### notifySenderDelivered (chat.message.delivered)
```java
- Receives delivery event from Kafka
- Pushes status to sender via /queue/status
- Payload: {messageId, delivered: true, deliveredAt}
```

#### notifySenderRead (chat.message.read)
```java
- Receives read event from Kafka
- Pushes status to sender via /queue/status
- Payload: {messageId, read: true, readAt}
```

## Status Icons

- **Single Tick (grey)**: Message sent but not delivered
- **Double Tick (grey)**: Message delivered to recipient
- **Double Tick (blue)**: Message read by recipient

## Database Persistence

All message statuses are persisted in PostgreSQL:
- `message_id` (primary key)
- `sender_id`
- `receiver_id`
- `message_text`
- `timestamp`
- `is_delivered` (boolean)
- `is_read` (boolean)
- `delivered_at` (timestamp)
- `read_at` (timestamp)

When users reconnect, they load messages with current status from DB.

## Testing Checklist

- [ ] A sends to B (B online, in chat) → Blue tick immediately
- [ ] A sends to B (B online, not in chat) → Grey double tick
- [ ] A sends to B (B offline) → Single tick
- [ ] B comes online later → A's tick updates to double
- [ ] B opens chat later → A's tick changes to blue
- [ ] A offline, B marks read → A sees blue when reconnects
- [ ] Multiple messages in sequence work correctly
- [ ] No duplicate messages in UI
- [ ] Status updates persist across page refreshes

## Key Improvements

1. **Removed optimistic UI updates** - Backend pushes to both sender and receiver
2. **Simplified message handler** - Clear logic for sender vs receiver
3. **Proper duplicate detection** - Checks messageId before adding
4. **Status updates work independently** - Separate subscribers for delivery/read
5. **Offline status handling** - Backend checks SimpUserRegistry
6. **DB persistence** - Status survives disconnects/reconnects
7. **Clean logging** - Every step logged for debugging
