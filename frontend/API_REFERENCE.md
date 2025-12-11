# Quick Reference - Backend Integration

## API Endpoints Summary

### User Service: `http://localhost:8080/user`

```typescript
// Register User
POST /user
Body: {
  firstName: string,
  lastName: string,
  email: string,
  userName: string,
  password: string
}
Response: "User Created" (HTTP 201)

// Get All Users
GET /user
Response: UserDto[]

// Get User by Username
GET /user/{userName}
Response: UserDto

// Login
GET /user/login?userName=xxx&password=yyy
Response: true (HTTP 200) or false (HTTP 401)
```

---

### Message Service: `http://localhost:8080/message`

```typescript
// Send Message
POST /message
Body: {
  messageText: string,
  senderId: number,
  receiverId: number,
  isDelivered: boolean,
  isRead: boolean
}
Response: {
  status: "success",
  message: "Message created successfully",
  messageId: number
}

// Get Messages Between Users
GET /message?senderId=X&receiverId=Y
Response: MessageDto[]

// Update Delivery Status
PUT /message/{messageId}/delivery
Response: {
  status: "success",
  message: "Message marked as delivered"
}

// Update Read Status
PUT /message/{messageId}/read
Response: {
  status: "success",
  message: "Message marked as read"
}
```

---

## Frontend Services Quick API

### Auth Service
```typescript
// Register
authService.register(user).subscribe(response => { /* ... */ })

// Login
authService.login(userName, password).subscribe(isValid => { /* ... */ })

// Get User Details
authService.getUserByUserName(userName).subscribe(user => { /* ... */ })

// Get All Users
authService.getAllUsers().subscribe(users => { /* ... */ })

// Set Current User
authService.setCurrentUser(user)

// Get Current User
const user = authService.getCurrentUser()

// Logout
authService.logout()
```

### Message Service
```typescript
// Get Messages
messageService.getMessages(senderId, receiverId)
  .subscribe(messages => { /* MessageDto[] */ })

// Send Message
messageService.sendMessage(messageDto)
  .subscribe(response => { /* MessageDto */ })

// Update Delivery Status
messageService.updateDeliveryStatus(messageId)
  .subscribe(response => { /* {...} */ })

// Update Read Status
messageService.updateReadStatus(messageId)
  .subscribe(response => { /* {...} */ })

// Mark All as Read
messageService.markMessagesAsRead(senderId, receiverId)
  .subscribe(response => { /* {...} */ })

// Convert DTO to Message
const message = messageService.convertDtoToMessage(dto, senderName)

// Convert Message to DTO
const dto = messageService.convertMessageToDto(message)
```

---

## User Flow

```
1. User opens app → /login page

2. New User:
   - Click "Register"
   - Fill form (firstName, lastName, email, userName, password)
   - Submit → POST /user
   - Redirected to /login

3. Existing User:
   - Enter userName & password
   - Submit → GET /user/login
   - If valid: GET /user/{userName} to fetch details
   - Store in AuthService
   - Redirected to /chat

4. In Chat:
   - GET /user (get all users)
   - Create conversations dynamically
   - Select user → GET /message (senderId, receiverId)
   - Type message → POST /message
   - Auto-update to "delivered" → PUT /message/{id}/delivery
   - When received/read → PUT /message/{id}/read

5. Logout:
   - Clear localStorage
   - Redirect to /login
```

---

## Important Notes

### ⚠️ User ID Handling
- Backend MUST return `userId` field in UserDto
- Frontend stores this for sending messages
- Currently defaults to 1 if not provided (needs fixing on backend)

### ⚠️ Message Timestamps
- Backend should assign timestamps (not client-side)
- Frontend displays timestamps from MessageDto

### ✅ Conversation Management
- Frontend dynamically creates conversations from all users
- No separate "add conversation" endpoint needed
- Messages persist in backend database (MySQL)

### ✅ Message Status Flow
- Sent → Delivered (auto-update 500ms after send)
- Delivered → Read (when recipient opens conversation)

---

## Files Modified for Integration

```
frontend/
├── src/
│   ├── environments/
│   │   ├── environment.ts          [UPDATED]
│   │   └── environment.prod.ts     [UPDATED]
│   ├── app/
│   │   ├── models/
│   │   │   └── chat.model.ts       [UPDATED]
│   │   ├── services/
│   │   │   ├── auth.service.ts     [UPDATED]
│   │   │   └── message.service.ts  [UPDATED]
│   │   └── components/
│   │       ├── auth/
│   │       │   ├── login.component.ts   [UPDATED]
│   │       │   ├── login.component.html [UPDATED]
│   │       │   ├── register.component.ts [UPDATED]
│   │       │   └── register.component.html [UPDATED]
│   │       └── chat/
│   │           └── chat.component.ts    [UPDATED]
└── INTEGRATION_SUMMARY.md          [NEW]
```

---

## Error Handling

### Registration Errors
- Duplicate userName → 409/400 from backend
- Invalid email → Caught by frontend validator
- Password mismatch → Caught by frontend validator

### Login Errors
- Invalid credentials → 401 response
- User not found → 404 response

### Message Errors
- Send failure → Log to console, notify user
- Load failure → Empty message list

---

## Next Steps (Optional)

1. **WebSocket Integration**: Replace HTTP polling with WebSocket for real-time messages
2. **Kafka Consumer**: Frontend webhook to receive Kafka events
3. **File Upload**: Support image/file messages
4. **User Search**: Search users by name
5. **Group Chat**: Add group messaging support
6. **Message Editing**: Edit/delete sent messages
7. **User Status**: Show online/offline status
8. **Typing Indicator**: Show when user is typing
