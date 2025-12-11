# Angular Frontend - Backend Integration Summary

## ✅ Integration Complete

The Angular frontend has been successfully integrated with the **user-service** and **message-service** backend APIs.

---

## API Endpoints Integrated

### User Service API (Backend: Port 0 - Registered with Eureka)
**Base URL:** `http://localhost:8080/user`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/user` | Register new user |
| `GET` | `/user` | Get all users |
| `GET` | `/user/{userName}` | Get user by username |
| `GET` | `/user/login` | Login user (params: userName, password) |

**Request Model:**
```json
{
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "userName": "string",
  "password": "string"
}
```

---

### Message Service API (Backend: Port 0 - Registered with Eureka)
**Base URL:** `http://localhost:8080/message`

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/message` | Save new message |
| `GET` | `/message` | Get messages (params: senderId, receiverId) |
| `PUT` | `/message/{messageId}/delivery` | Update delivery status |
| `PUT` | `/message/{messageId}/read` | Update read status |

**Request Model:**
```json
{
  "messageText": "string",
  "senderId": "number",
  "receiverId": "number",
  "isDelivered": "boolean",
  "isRead": "boolean"
}
```

---

## Frontend Services Updated

### 1. **Auth Service** (`src/app/services/auth.service.ts`)
- `register(user)` - Register new user with user-service
- `login(userName, password)` - Validate user credentials
- `getUserByUserName(userName)` - Fetch user details
- `getAllUsers()` - Get all users for conversation list
- `setCurrentUser(user)` - Store current user in local storage
- `getCurrentUser()` - Retrieve current user
- `logout()` - Clear session

### 2. **Message Service** (`src/app/services/message.service.ts`)
- `getMessages(senderId, receiverId)` - Fetch conversation messages
- `sendMessage(messageDto)` - Send new message
- `updateDeliveryStatus(messageId)` - Mark message as delivered
- `updateReadStatus(messageId)` - Mark message as read
- `markMessagesAsRead(senderId, receiverId)` - Batch mark as read
- Helper methods:
  - `convertDtoToMessage()` - Convert MessageDto to Message UI model
  - `convertMessageToDto()` - Convert Message to MessageDto API model

---

## Components Updated

### 1. **Login Component** (`src/app/components/auth/login.component.ts`)
- Calls `/user/login` with userName and password
- On success, fetches full user details
- Stores user in auth service
- Redirects to chat page

### 2. **Register Component** (`src/app/components/auth/register.component.ts`)
- Sends user data to `/user` POST endpoint
- Validates form (matching passwords, email format)
- Redirects to login on success

### 3. **Chat Component** (`src/app/components/chat/chat.component.ts`)
- Loads all users from `/user` GET endpoint
- Creates conversations with each user
- Fetches messages with `/message` GET endpoint
- Sends messages to `/message` POST endpoint
- Updates message status via delivery/read endpoints
- Displays message status with icons:
  - ✓ Single tick = Sent
  - ✓✓ Double tick = Delivered
  - ✓✓ (Blue) = Read

---

## Data Models

### User Interface
```typescript
interface User {
  firstName: string;
  lastName: string;
  email: string;
  userName: string;
  password?: string;
  userId?: number;
}
```

### MessageDto Interface
```typescript
interface MessageDto {
  messageId?: number;
  messageText: string;
  senderId: number;
  receiverId: number;
  isDelivered: boolean;
  isRead: boolean;
}
```

---

## Flow Diagram

```
1. REGISTRATION
   User Form → Register Component → POST /user → User-Service

2. LOGIN
   User Form → Login Component → GET /user/login → User-Service
   ↓ (on success)
   GET /user/{userName} → User-Service
   ↓ (store user)
   Auth Service (local storage)

3. CHAT
   Load Users → GET /user → Auth Service
   ↓
   Create Conversations
   ↓
   Select Conversation → GET /message (senderId, receiverId)
   ↓
   Send Message → POST /message → Message-Service
   ↓
   Update Status → PUT /message/{id}/delivery → Message-Service
   ↓
   Mark as Read → PUT /message/{id}/read → Message-Service
```

---

## Environment Configuration

**File:** `src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiGateway: 'http://localhost:8080',
  userServiceUrl: 'http://localhost:8080/user',
  messageServiceUrl: 'http://localhost:8080/message'
};
```

The API Gateway at port 8080 will route requests to the respective services registered with Eureka.

---

## Required Backend Services Running

For the frontend to work, ensure these services are running:

1. **Eureka Server** - Port 8123
2. **API Gateway** - Port 8080 (routes to user-service and message-service)
3. **User Service** - Dynamic port (registered with Eureka)
4. **Message Service** - Dynamic port (registered with Eureka)
5. **MySQL** - Database for both services
6. **Kafka** - For event messaging (message-service)
7. **Redis** - For caching (message-service)

---

## Key Features Implemented

✅ User registration with validation  
✅ User login with authentication  
✅ Conversation list with all users  
✅ Real-time messaging  
✅ Message delivery status tracking  
✅ Message read status tracking  
✅ Auto-scroll to latest messages  
✅ User logout  
✅ Responsive UI with Bootstrap 5  

---

## Known Clarifications

### User ID Assignment
- User ID should be auto-generated on backend during registration
- Frontend assumes userId is available from `/user/{userName}` response
- **Action Required:** Backend should return userId in UserDto response

### Message Timestamps
- Current implementation uses client-side timestamps
- **Recommendation:** Backend should assign timestamps to ensure consistency

### Conversation Loading
- Currently loads all users and creates conversations dynamically
- **Future Enhancement:** Backend could provide pre-loaded conversation list with last message info

### Kafka Integration
- Message-service uses Kafka for event publishing
- Frontend subscribes via HTTP polling (calling GET /message)
- **Future Enhancement:** Implement WebSocket for real-time push notifications

---

## Testing the Integration

### Step 1: Start Backend Services
```bash
# Ensure Eureka, API Gateway, User-Service, and Message-Service are running
```

### Step 2: Install Frontend Dependencies
```bash
cd frontend
npm install
```

### Step 3: Start Frontend
```bash
npm start
# Opens http://localhost:4200
```

### Step 4: Test Flow
1. Register a new user
2. Login with credentials
3. See list of other users as conversations
4. Send messages
5. Verify message status updates

---

## Troubleshooting

### CORS Issues
- API Gateway should have CORS enabled
- Check API Gateway configuration

### 404 Errors
- Verify Eureka has registered user-service and message-service
- Check API Gateway routing rules

### Message Not Sending
- Verify message-service is running
- Check Kafka is running (for event publishing)
- Check MySQL database has messageservicedb

### Users Not Loading
- Verify user-service is running
- Check MySQL database has userservicedb

---

## Notes

- Frontend is fully compatible with Node 20+
- Uses Angular 18 with TypeScript 5.5.4
- No external authentication library (simple HTTP-based)
- Bootstrap 5.3 for styling
- Responsive design for mobile and desktop
