# Chat Application - Test Cases

## Test Execution Status Legend
- ✅ PASS - Feature working as expected
- ❌ FAIL - Feature not working
- ⚠️ PARTIAL - Partially working
- ⏳ PENDING - Not tested yet

---

## 1. Authentication & User Management

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| AUTH-01 | User Registration | 1. Open registration page<br>2. Enter: First Name, Last Name, Email, Username, Password<br>3. Click Register | User registered successfully, redirected to login | ⏳ | |
| AUTH-02 | User Login - Valid Credentials | 1. Open login page<br>2. Enter valid username and password<br>3. Click Login | User logged in, redirected to chat page | ⏳ | |
| AUTH-03 | User Login - Invalid Credentials | 1. Open login page<br>2. Enter invalid username/password<br>3. Click Login | Error message displayed, login failed | ⏳ | |
| AUTH-04 | User Session Persistence | 1. Login successfully<br>2. Close browser<br>3. Reopen application | User still logged in (from localStorage) | ⏳ | |
| AUTH-05 | User Logout | 1. Login successfully<br>2. Click logout button | User logged out, redirected to login page | ⏳ | |

---

## 2. WebSocket Connection & Real-Time Communication

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| WS-01 | WebSocket Connection on Login | 1. Login with valid credentials<br>2. Check browser console | WebSocket connected successfully, logs show "Connected" | ⏳ | Check for STOMP CONNECTED frame |
| WS-02 | Presence Announcement | 1. Login as User A<br>2. Check backend logs | Presence announced: "User X is online" | ⏳ | |
| WS-03 | WebSocket Reconnection | 1. Login successfully<br>2. Stop message-service<br>3. Start message-service | WebSocket automatically reconnects | ⏳ | |
| WS-04 | Multiple User Connections | 1. Login as User A (Browser 1)<br>2. Login as User B (Browser 2)<br>3. Check backend logs | Both users registered in SimpUserRegistry | ⏳ | |

---

## 3. Online/Offline Status

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| PRES-01 | Online Status on Login | 1. Login as User A<br>2. Open another browser, login as User B<br>3. Check User A's screen | User B shows "Online" with green dot | ⏳ | |
| PRES-02 | Offline Status on Logout | 1. User A and User B both online<br>2. User B logs out<br>3. Check User A's screen | User B shows "Offline" with grey dot | ⏳ | |
| PRES-03 | Initial Online Users List | 1. User A already logged in<br>2. Login as User B<br>3. Check User B's screen | User A immediately shows as "Online" | ⏳ | Backend sends existing online users |
| PRES-04 | Real-time Presence Updates | 1. User A and B both logged in<br>2. User B closes browser<br>3. Wait 30 seconds<br>4. Check User A's screen | User B changes to "Offline" | ⏳ | |

---

## 4. Message Sending & Receiving

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| MSG-01 | Send Message - Basic | 1. User A logged in<br>2. Select User B from list<br>3. Type "Hello"<br>4. Click Send | Message appears in A's chat immediately | ⏳ | Optimistic UI update |
| MSG-02 | Receive Message - Real-time | 1. User A and B both logged in<br>2. Both open chat with each other<br>3. User A sends "Hello"<br>4. Check User B's screen | Message appears in B's chat in real-time | ⏳ | |
| MSG-03 | Message Saved in Database | 1. User A sends message to B<br>2. Check PostgreSQL database<br>3. Run: `SELECT * FROM message ORDER BY created_date DESC LIMIT 1;` | Message exists with correct sender_id, receiver_id, message_text | ⏳ | |
| MSG-04 | Empty Message Validation | 1. User A opens chat with B<br>2. Try to send empty message<br>3. Click Send | Message not sent, validation error (if implemented) | ⏳ | |
| MSG-05 | Long Message Handling | 1. User A types message >500 characters<br>2. Send message | Message sent and displayed correctly | ⏳ | |

---

## 5. Message Status & Receipts (WhatsApp-like)

### Single Tick (Sent)
| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| STAT-01 | Single Tick - Receiver Offline | 1. User A logged in<br>2. User B logged OUT<br>3. User A sends message to B<br>4. Check message status icon | Single grey tick (✓) displayed | ⏳ | |

### Double Grey Tick (Delivered)
| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| STAT-02 | Double Tick - Receiver Online but Chat Closed | 1. User A logged in<br>2. User B logged in but NOT in chat with A<br>3. User A sends message to B<br>4. Check message status icon in A's chat | Double grey tick (✓✓) displayed | ⏳ | |
| STAT-03 | Status Update - Receiver Comes Online | 1. User A sends message to B (B offline)<br>2. Single tick displayed<br>3. User B logs in<br>4. Check message status in A's chat | Changes from single tick to double grey tick | ⏳ | |

### Blue Double Tick (Read)
| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| STAT-04 | Blue Tick - Receiver Opens Chat | 1. User A sends message to B<br>2. Message shows double grey tick<br>3. User B opens chat with A<br>4. Check message status in A's chat | Changes to blue double tick (✓✓) | ⏳ | |
| STAT-05 | Blue Tick - Immediate if Chat Open | 1. User A and B both in chat with each other<br>2. User A sends message<br>3. Check message status in A's chat | Blue double tick (✓✓) immediately | ⏳ | |

### Status Persistence
| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| STAT-06 | Status Persists After Page Refresh | 1. User A sends message to B<br>2. B opens chat (blue tick appears)<br>3. User A refreshes page<br>4. Check message status | Still shows blue double tick | ⏳ | From database |
| STAT-07 | Status Persists When Switching Conversations | 1. User A sends message to B (blue tick)<br>2. User A switches to User C chat<br>3. User A switches back to B chat<br>4. Check message status | Still shows blue double tick | ⏳ | From cache or database |

---

## 6. Conversation List

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| CONV-01 | Load Conversations on Login | 1. User A logs in<br>2. Check conversation list | All other users appear in conversation list | ⏳ | |
| CONV-02 | Show Last Message in List | 1. User A logs in<br>2. Previous messages exist with User B<br>3. Check conversation list | Last message preview shown under User B's name | ⏳ | |
| CONV-03 | Unread Count Display | 1. User B sends 3 messages to A (A not in chat)<br>2. User A logs in<br>3. Check conversation list | Badge showing "3" next to User B's name | ⏳ | |
| CONV-04 | Unread Count Clears on Open | 1. User A has 3 unread from B<br>2. User A clicks on B's conversation<br>3. Check badge | Unread count badge disappears (0) | ⏳ | |
| CONV-05 | Sort by Latest Message | 1. User A has chats with B, C, D<br>2. User C sends new message<br>3. Check conversation list | User C moves to top of list | ⏳ | Most recent first |
| CONV-06 | Real-time List Update | 1. User A in chat app<br>2. User B sends message<br>3. Check A's conversation list | User B moves to top, last message updates | ⏳ | |

---

## 7. Message History & Loading

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| HIST-01 | Load Previous Messages | 1. User A and B have 10+ messages in history<br>2. User A logs in fresh<br>3. Click on User B conversation | All previous messages loaded and displayed | ⏳ | From database |
| HIST-02 | Message Order - Chronological | 1. Load conversation with history<br>2. Check message order | Messages displayed oldest to newest (top to bottom) | ⏳ | |
| HIST-03 | Sender vs Receiver Alignment | 1. Load conversation<br>2. Check message alignment | Sent messages on right, received on left | ⏳ | |
| HIST-04 | Timestamp Display | 1. Load conversation<br>2. Check timestamps | Each message shows timestamp | ⏳ | |

---

## 8. Chat Active Status (Backend Tracking)

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| CHAT-01 | Mark Messages Read on Chat Open | 1. User A sends 5 messages to B<br>2. All show double grey tick<br>3. User B opens chat with A<br>4. Check A's screen | All 5 messages change to blue tick | ⏳ | |
| CHAT-02 | Set Chat Active Status | 1. User B opens chat with A<br>2. Check backend logs | "User B active with peer A" logged | ⏳ | |
| CHAT-03 | Clear Chat Active on Switch | 1. User B in chat with A<br>2. User B clicks on User C<br>3. Check backend logs | "User B cleared active chat" logged | ⏳ | |

---

## 9. Multi-User Scenarios

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| MULTI-01 | Three Users Online | 1. Login as A, B, C (3 browsers)<br>2. Check each user's screen | All show each other as online | ⏳ | |
| MULTI-02 | Message Between A-B, A-C Separately | 1. A sends "Hi B" to B<br>2. A sends "Hi C" to C<br>3. Check B and C screens | B sees only "Hi B", C sees only "Hi C" | ⏳ | Proper message routing |
| MULTI-03 | Status Updates Don't Cross | 1. A sends to B (blue tick)<br>2. A sends to C (grey tick)<br>3. Check status icons | Each conversation has independent status | ⏳ | |

---

## 10. Edge Cases & Error Handling

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| EDGE-01 | Send Message Without WebSocket | 1. Login<br>2. Stop message-service<br>3. Try to send message | Error handling or queuing | ⏳ | |
| EDGE-02 | Duplicate Message Prevention | 1. User A sends message<br>2. Message arrives twice via WebSocket<br>3. Check A's chat | Message appears only once | ⏳ | |
| EDGE-03 | Special Characters in Message | 1. Send message with emojis 😊🎉<br>2. Send message with quotes "'<br>3. Check receiver's screen | All characters display correctly | ⏳ | |
| EDGE-04 | Concurrent Messages | 1. User A and B in chat<br>2. Both send message at exact same time<br>3. Check both screens | Both messages delivered correctly | ⏳ | |
| EDGE-05 | Backend Restart During Chat | 1. Active chat session<br>2. Restart message-service<br>3. Try sending message | WebSocket reconnects, message sent | ⏳ | |

---

## 11. Database Persistence

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| DB-01 | Messages Persist After Logout | 1. User A sends 3 messages to B<br>2. Both logout<br>3. Both login again<br>4. Check conversation | All 3 messages still visible | ⏳ | |
| DB-02 | Delivery Status in Database | 1. A sends to B, delivered<br>2. Check database: `SELECT is_delivered FROM message WHERE message_id = X;` | is_delivered = true | ⏳ | |
| DB-03 | Read Status in Database | 1. A sends to B, B reads<br>2. Check database: `SELECT is_read FROM message WHERE message_id = X;` | is_read = true | ⏳ | |

---

## 12. Performance & Scalability

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| PERF-01 | Load 100+ Messages | 1. Create 100+ messages in conversation<br>2. Login and open conversation<br>3. Measure load time | Messages load within 3 seconds | ⏳ | |
| PERF-02 | Send 10 Messages Rapidly | 1. Send 10 messages in quick succession<br>2. Check receiver's screen | All 10 delivered correctly, no lag | ⏳ | |
| PERF-03 | 10 Concurrent Users | 1. Login with 10 users simultaneously<br>2. All send messages<br>3. Check backend logs | All messages processed correctly | ⏳ | |

---

## 13. UI/UX Validation

| Test ID | Test Case | Steps to Execute | Expected Result | Status | Notes |
|---------|-----------|------------------|-----------------|--------|-------|
| UI-01 | Sent Message Alignment | 1. Send message<br>2. Check alignment | Message appears on RIGHT side with green background | ⏳ | |
| UI-02 | Received Message Alignment | 1. Receive message<br>2. Check alignment | Message appears on LEFT side with white background | ⏳ | |
| UI-03 | Status Icon Display | 1. Check status icons<br>2. Verify icons | Single tick: ✓<br>Double grey: ✓✓<br>Blue double: ✓✓ (blue) | ⏳ | |
| UI-04 | Online Status Indicator | 1. Check online user<br>2. Check offline user | Online: Green dot + "Online"<br>Offline: Grey dot + "Offline" | ⏳ | |
| UI-05 | Scroll to Bottom on New Message | 1. Long conversation (scrollable)<br>2. New message arrives<br>3. Check scroll position | Auto-scrolls to show new message | ⏳ | |
| UI-06 | Input Field Clears After Send | 1. Type message<br>2. Click Send<br>3. Check input field | Input field is empty and ready for next message | ⏳ | |

---

## Test Execution Summary

| Category | Total Tests | Passed | Failed | Pending |
|----------|-------------|--------|--------|---------|
| Authentication | 5 | | | 5 |
| WebSocket | 4 | | | 4 |
| Presence | 4 | | | 4 |
| Messaging | 5 | | | 5 |
| Status & Receipts | 7 | | | 7 |
| Conversation List | 6 | | | 6 |
| Message History | 4 | | | 4 |
| Chat Active | 3 | | | 3 |
| Multi-User | 3 | | | 3 |
| Edge Cases | 5 | | | 5 |
| Database | 3 | | | 3 |
| Performance | 3 | | | 3 |
| UI/UX | 6 | | | 6 |
| **TOTAL** | **58** | **0** | **0** | **58** |

---

## How to Use This Test Document

1. **Execute each test case** following the steps
2. **Update Status column** with:
   - ✅ if working correctly
   - ❌ if not working
   - ⚠️ if partially working
3. **Add Notes** for any issues found
4. **Update Summary table** at the end

## Priority Testing Order (Recommended)

1. **Authentication** (AUTH-01 to AUTH-05) - Must work first
2. **WebSocket Connection** (WS-01, WS-02) - Core functionality
3. **Basic Messaging** (MSG-01, MSG-02, MSG-03) - Core feature
4. **Message Status** (STAT-01 to STAT-07) - Main requirement
5. **Conversation List** (CONV-01 to CONV-06) - User experience
6. **Online Status** (PRES-01 to PRES-04) - Important feature
7. **Remaining tests** - Additional validation

---

**Document Version:** 1.0  
**Last Updated:** December 10, 2025  
**Tested By:** _________________  
**Test Environment:** Local Development (Spring Boot + Angular + PostgreSQL + Kafka + Redis)
