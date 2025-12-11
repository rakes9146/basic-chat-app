import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { MessageService } from '../../services/message.service';
import { AuthService } from '../../services/auth.service';
import { WebsocketService, WebSocketMessage, MessageStatus } from '../../services/websocket.service';
import { Router } from '@angular/router';
import { Message, Conversation, User, MessageDto } from '../../models/chat.model';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-chat',
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss']
})
export class ChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  conversations: Conversation[] = [];
  selectedConversation: Conversation | null = null;
  messages: Message[] = [];
  messageText = '';
  currentUser: User | null = null;
  allUsers: User[] = [];
  loading = false;
  wsConnected = false;
  onlineUsers = new Set<number>(); // Track online users
  private messageStatusCache = new Map<number, 'sent' | 'delivered' | 'read'>(); // Cache message status
  private destroy$ = new Subject<void>();
  private shouldScroll = false;
  private messageSubscription: any;
  private deliveryStatusSubscription: any;
  private readStatusSubscription: any;

  constructor(
    private messageService: MessageService,
    private authService: AuthService,
    private websocketService: WebsocketService,
    private router: Router
  ) {}

  ngOnInit(): void {
    console.log('='.repeat(80));
    console.log('[CHAT COMPONENT] 🚀 INITIALIZING');
    console.log('='.repeat(80));
    
    // Get current user immediately (might be in localStorage)
    const currentUser = this.authService.getCurrentUser();
    console.log('[CHAT COMPONENT] 👤 Current user from localStorage:', JSON.stringify(currentUser, null, 2));
    
    if (currentUser && currentUser.userId) {
      this.currentUser = currentUser;
      console.log(`[CHAT COMPONENT] ✅ User ${currentUser.userId} already logged in`);
      console.log('[CHAT COMPONENT] 🔌 Connecting to WebSocket with userId:', currentUser.userId);
      
      // Connect to WebSocket with userId for proper session identification
      this.websocketService.connect(currentUser.userId);
      console.log('[CHAT COMPONENT] 📡 WebSocket connect() called');
      
      // Load users immediately
      console.log('[CHAT COMPONENT] 📋 Loading all users');
      this.loadAllUsers();
    } else {
      console.warn('[CHAT COMPONENT] ⚠️ No user in localStorage, waiting for currentUser$ observable');
    }
    
    // Also subscribe to currentUser$ for future updates
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe(user => {
      console.log('[CHAT COMPONENT] 👤 User from currentUser$ observable:', JSON.stringify(user, null, 2));
      
      if (user && user.userId && user.userId !== this.currentUser?.userId) {
        this.currentUser = user;
        console.log(`[CHAT COMPONENT] ✅ User ${user.userId} logged in via observable`);
        console.log('[CHAT COMPONENT] 🔌 Connecting to WebSocket with userId:', user.userId);
        
        // Connect to WebSocket with userId for proper session identification
        this.websocketService.connect(user.userId);
        console.log('[CHAT COMPONENT] 📡 WebSocket connect() called');
        
        // Load users
        console.log('[CHAT COMPONENT] 📋 Loading all users');
        this.loadAllUsers();
      }
    });
    
    // Monitor WebSocket connection status
    this.websocketService.isConnected$.pipe(takeUntil(this.destroy$))
      .subscribe(isConnected => {
        console.log('[CHAT COMPONENT] 🔄 WebSocket connection status changed:', isConnected);
        this.wsConnected = isConnected;
        if (isConnected && this.currentUser) {
          console.log('[CHAT COMPONENT] ✅ WebSocket CONNECTED - Setting up subscriptions');
          
          // Subscribe to presence FIRST before announcing our presence
          this.subscribeToPresence();
          
          // Announce presence online when connected
          console.log('[CHAT COMPONENT] 📣 Announcing presence for user:', this.currentUser.userId);
          this.websocketService.announcePresence(this.currentUser.userId!, true);
          
          // Subscribe to messages and status updates
          console.log('[CHAT COMPONENT] 📬 Subscribing to WebSocket messages');
          this.subscribeToWebSocketMessages();
          console.log('[CHAT COMPONENT] ✅ Subscriptions complete');
        } else if (!isConnected) {
          console.warn('[CHAT COMPONENT] ⚠️ WebSocket DISCONNECTED');
        }
      });

    // Monitor connection errors
    this.websocketService.connectionError$.pipe(takeUntil(this.destroy$))
      .subscribe(error => {
        console.error('WebSocket error:', error);
      });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  ngOnDestroy(): void {
    // Announce presence offline before disconnecting
    if (this.currentUser?.userId && this.wsConnected) {
      this.websocketService.announcePresence(this.currentUser.userId, false);
      
      // Clear chat activity if a conversation is selected
      if (this.selectedConversation) {
        this.websocketService.setChatActive(
          this.currentUser.userId,
          this.selectedConversation.participantId,
          false
        );
      }
    }
    
    // Unsubscribe from WebSocket topics
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

  /**
   * Subscribe to WebSocket messages and status updates
   */
  private subscribeToWebSocketMessages(): void {
    console.log('[SUBSCRIBE] 🎯 subscribeToWebSocketMessages() called');
    
    if (!this.currentUser?.userId) {
      console.error('[SUBSCRIBE] ❌ No userId available');
      return;
    }

    const userId = this.currentUser.userId;
    console.log('[SUBSCRIBE] 👤 Subscribing for userId:', userId);

    // Subscribe to incoming messages
    console.log('[SUBSCRIBE] 📨 Calling subscribeToMessages()');
    this.messageSubscription = this.websocketService.subscribeToMessages(userId);
    console.log('[SUBSCRIBE] ✅ Message subscription created:', this.messageSubscription);

    console.log('[SUBSCRIBE] 🎧 Setting up messages$ observable listener');
    this.websocketService.messages$.pipe(takeUntil(this.destroy$))
      .subscribe((wsMessage: any) => {
        console.log('\n' + '='.repeat(80));
        console.log('[MESSAGE RECEIVED] 📨 New WebSocket Message');
        console.log('='.repeat(80));
        console.log('[MESSAGE] Raw:', JSON.stringify(wsMessage, null, 2));
        
        const currentUserId = this.currentUser?.userId;
        if (!currentUserId) {
          console.error('[MESSAGE] ❌ No current user');
          return;
        }
        
        const isSender = wsMessage.senderId === currentUserId;
        const isReceiver = wsMessage.receiverId === currentUserId;
        
        console.log('[MESSAGE] Analysis: currentUser=%d, sender=%d, receiver=%d, isSender=%s, isReceiver=%s',
          currentUserId, wsMessage.senderId, wsMessage.receiverId, isSender, isReceiver);
        
        if (!isSender && !isReceiver) {
          console.log('[MESSAGE] ❌ Not relevant, ignoring');
          return;
        }

        // Check if message already exists (by real messageId, not temporary id=0)
        const existingMessage = this.messages.find(m => 
          m.id === wsMessage.messageId && wsMessage.messageId !== 0
        );
        if (existingMessage) {
          console.log('[MESSAGE] ⚠️ Duplicate detected, ignoring ID:', wsMessage.messageId);
          return;
        }

        // If this is a sender confirmation (echo), replace optimistic message
        if (isSender) {
          const optimisticIndex = this.messages.findIndex(m => 
            m.id === 0 && 
            m.senderId === wsMessage.senderId && 
            m.receiverId === wsMessage.receiverId &&
            m.content === wsMessage.messageText
          );
          
          if (optimisticIndex !== -1) {
            console.log('[MESSAGE] 🔄 Replacing optimistic message at index', optimisticIndex);
            this.messages[optimisticIndex].id = wsMessage.messageId;
            this.messages[optimisticIndex].timestamp = wsMessage.timestamp || new Date();
            // Preserve or update status from server
            if (wsMessage.isRead) {
              this.messages[optimisticIndex].status = 'read';
            } else if (wsMessage.isDelivered) {
              this.messages[optimisticIndex].status = 'delivered';
            }
            console.log('[MESSAGE] ✅ Optimistic message confirmed with ID:', wsMessage.messageId, 'status:', this.messages[optimisticIndex].status);
            
            // Update conversation list for sender (since we're returning early)
            if (isSender) {
              this.updateConversationLastMessage(wsMessage.receiverId, wsMessage.messageText, false);
            }
            return; // Don't add duplicate
          }
        }

        // Determine conversation partner: if I'm sender, partner is receiver; if I'm receiver, partner is sender
        const partnerId = isSender ? wsMessage.receiverId : wsMessage.senderId;
        
        console.log('[MESSAGE] Partner ID:', partnerId);

        // Get sender name
        const senderUser = this.allUsers.find(u => u.userId === wsMessage.senderId);
        const senderName = senderUser 
          ? `${senderUser.firstName} ${senderUser.lastName}` 
          : (isSender ? `${this.currentUser?.firstName} ${this.currentUser?.lastName}` : 'Unknown');

        // Create message object
        const message: Message = {
          id: wsMessage.messageId,
          senderId: wsMessage.senderId,
          senderName: senderName,
          receiverId: wsMessage.receiverId,
          content: wsMessage.messageText,
          timestamp: wsMessage.timestamp || new Date(),
          status: 'sent' // Initial status - will be updated via status updates
        };

        console.log('[MESSAGE] Created:', message);

        // Add to UI ONLY if the conversation with the partner is currently open
        if (this.selectedConversation?.participantId === partnerId) {
          console.log('[MESSAGE] ✅ Adding to open conversation');
          this.messages.push(message);
          this.shouldScroll = true;
          console.log('[MESSAGE] Messages count:', this.messages.length);
          
          // If I'm the receiver and chat is open, mark as delivered and read
          if (isReceiver && wsMessage.messageId) {
            console.log('[DELIVERY] 📬 Marking as delivered (receiver, chat open)');
            this.websocketService.markMessageDelivered(wsMessage.messageId, currentUserId);
            
            console.log('[READ] 👁️ Marking as read (receiver, chat open)');
            this.websocketService.markMessageRead(wsMessage.messageId, currentUserId);
          }
        } else {
          console.log('[MESSAGE] 💬 Different conversation open, not adding to UI');
          console.log('[MESSAGE] Selected:', this.selectedConversation?.participantId, 'Partner:', partnerId);
          
          // If I'm the receiver but chat NOT open, still mark as delivered (but not read)
          if (isReceiver && wsMessage.messageId) {
            console.log('[DELIVERY] 📬 Marking as delivered (receiver, chat NOT open)');
            this.websocketService.markMessageDelivered(wsMessage.messageId, currentUserId);
          }
        }

        // Update conversation list
        if (isReceiver) {
          this.updateConversationLastMessage(wsMessage.senderId, wsMessage.messageText, true);
        } else if (isSender) {
          this.updateConversationLastMessage(wsMessage.receiverId, wsMessage.messageText, false);
        }
      });

    // Subscribe to delivery status updates
    console.log('[SUBSCRIBE] 📊 Setting up deliveryStatus$ listener');
    this.websocketService.subscribeToDeliveryStatus(userId); // Subscribe to /queue/status
    this.websocketService.deliveryStatus$.pipe(takeUntil(this.destroy$))
      .subscribe((status: any) => {
        console.log('\n' + '='.repeat(80));
        console.log('[DELIVERY STATUS] 📬 Received');
        console.log('='.repeat(80));
        console.log('[DELIVERY] Status:', JSON.stringify(status, null, 2));
        
        if (status.delivered && status.messageId) {
          // Cache the status
          this.messageStatusCache.set(status.messageId, 'delivered');
          
          const message = this.messages.find(m => m.id === status.messageId);
          if (message) {
            console.log('[DELIVERY] ✅ Updating message', status.messageId, 'to delivered');
            message.status = 'delivered';
          } else {
            console.log('[DELIVERY] ⚠️ Message', status.messageId, 'not found in current messages (cached for later)');
          }
        }
      });

    // Subscribe to read status updates
    console.log('[SUBSCRIBE] 👁️ Setting up readStatus$ listener');
    this.websocketService.readStatus$.pipe(takeUntil(this.destroy$))
      .subscribe((status: any) => {
        console.log('\n' + '='.repeat(80));
        console.log('[READ STATUS] 👁️ Received');
        console.log('='.repeat(80));
        console.log('[READ] Status:', JSON.stringify(status, null, 2));
        
        if (status.read && status.messageId) {
          // Cache the status
          this.messageStatusCache.set(status.messageId, 'read');
          
          const message = this.messages.find(m => m.id === status.messageId);
          if (message) {
            console.log('[READ] ✅ Updating message', status.messageId, 'to read');
            message.status = 'read';
            
            // Decrement unread count for the conversation this message belongs to
            // Find the conversation partner (if I sent the message, partner is receiver; if I received, partner is sender)
            const partnerId = (message.senderId === this.currentUser?.userId) ? message.receiverId : message.senderId;
            const conversation = this.conversations.find(c => c.participantId === partnerId);
            if (conversation && conversation.unreadCount > 0) {
              conversation.unreadCount = Math.max(0, conversation.unreadCount - 1);
              console.log('[READ] ✅ Decremented unread count for conversation:', conversation.participantName, '-> now', conversation.unreadCount);
            }
          } else {
            console.log('[READ] ⚠️ Message', status.messageId, 'not found in current messages (cached for later)');
          }
        }
      });

    console.log('[SUBSCRIBE] ✅ All subscriptions configured');
  }

  private subscribeToPresence(): void {
    console.log('[PRESENCE] 📡 Subscribing to user presence updates');
    
    // Subscribe to global presence notifications (all users)
    this.websocketService.subscribeToPresence();
    this.websocketService.presenceUpdates$.pipe(takeUntil(this.destroy$))
      .subscribe((update: any) => {
        console.log('[PRESENCE] 👤 Presence update:', update);
        
        if (update.userId) {
          if (update.online) {
            this.onlineUsers.add(update.userId);
            console.log('[PRESENCE] ✅ User', update.userId, 'is ONLINE');
          } else {
            this.onlineUsers.delete(update.userId);
            console.log('[PRESENCE] ⚪ User', update.userId, 'is OFFLINE');
          }
        }
      });
    
    // Subscribe to initial online users list (sent when we connect)
    if (this.currentUser?.userId) {
      this.websocketService.subscribeToPresenceInit(this.currentUser.userId);
      this.websocketService.presenceUpdates$.pipe(takeUntil(this.destroy$))
        .subscribe((update: any) => {
          if (update.userId && update.online) {
            this.onlineUsers.add(update.userId);
            console.log('[PRESENCE-INIT] ✅ User', update.userId, 'was already online');
          }
        });
    }
  }

  /**
   * Update message status in UI - works across all messages in current view
   */
  private updateMessageStatus(messageId: number, status: 'delivered' | 'read'): void {
    // Find the message in the current messages array (if conversation is open)
    const message = this.messages.find(m => m.id === messageId);
    if (message) {
      message.status = status;
      console.log(`[STATUS] ✅ Updated message ${messageId} status to ${status} in UI`);
    } else {
      console.log(`[STATUS] ℹ️ Message ${messageId} not in current view (different conversation)`);
    }
  }

  /**
   * Update conversation list with latest message
   */
  private updateConversationLastMessage(senderId: number, messageText: string, isReceived: boolean = false): void {
    const conversation = this.conversations.find(c => c.participantId === senderId);
    if (conversation) {
      conversation.lastMessage = messageText;
      conversation.lastMessageTime = new Date();
      
      const previousCount = conversation.unreadCount || 0;
      
      // Increment unread count ONLY if:
      // 1. Message is received (not sent by current user)
      // 2. Conversation is not currently selected
      // 3. Don't increment if conversation is already open (user will see it immediately)
      if (isReceived && this.selectedConversation?.participantId !== senderId) {
        conversation.unreadCount = previousCount + 1;
        console.log(`[UNREAD COUNT] ⬆️ Incremented for ${conversation.participantName}: ${previousCount} -> ${conversation.unreadCount} (message received, conversation not open)`);
      } else if (isReceived && this.selectedConversation?.participantId === senderId) {
        // Keep count at 0 if conversation is open (message is immediately read)
        conversation.unreadCount = 0;
        console.log(`[UNREAD COUNT] ✅ Keeping at 0 for ${conversation.participantName} (conversation is OPEN, message auto-read)`);
      } else {
        console.log(`[UNREAD COUNT] ➡️ No change for ${conversation.participantName} (count: ${previousCount}, isReceived: ${isReceived}, isSender: ${!isReceived})`);
      }
      
      // Sort conversations by latest message time (most recent first)
      this.sortConversations();
    }
  }

  /**
   * Sort conversations by most recent message time
   */
  private sortConversations(): void {
    this.conversations.sort((a, b) => {
      const timeA = a.lastMessageTime ? new Date(a.lastMessageTime).getTime() : 0;
      const timeB = b.lastMessageTime ? new Date(b.lastMessageTime).getTime() : 0;
      return timeB - timeA; // Descending order (most recent first)
    });
    console.log('[SORT] Conversations sorted by latest message');
  }

  loadAllUsers(): void {
    if (!this.currentUser) return;

    this.loading = true;
    this.authService.getAllUsers().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (users: any) => {
        console.log('[LOAD USERS] Received users:', users);
        // Filter out current user from the list
        this.allUsers = users.filter((u: any) => u.userName !== this.currentUser?.userName);
        console.log('[LOAD USERS] Filtered users (excluding current):', this.allUsers);
        this.createConversations();
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Error loading users:', err);
        this.loading = false;
      }
    });
  }

  createConversations(): void {
    console.log('[CREATE CONVERSATIONS] Creating conversations from users:', this.allUsers);
    this.conversations = this.allUsers.map(user => {
      const userAny = user as any;
      console.log('[CREATE CONVERSATIONS] User:', user, 'userId:', user.userId, 'id:', userAny.id);
      const participantId = user.userId || userAny.id || 0;
      console.log('[CREATE CONVERSATIONS] Assigned participantId:', participantId);
      return {
        id: user.userId || userAny.id || Math.random(),
        participantId: participantId,
        participantName: `${user.firstName} ${user.lastName}`,
        lastMessage: '',
        lastMessageTime: new Date(0), // Set to epoch so conversations without messages appear at bottom
        unreadCount: 0,
        messages: []
      };
    });
    console.log('[CREATE CONVERSATIONS] Final conversations:', this.conversations);
    
    // Load last message for each conversation
    this.loadLastMessagesForConversations();
  }

  /**
   * Load the last message for each conversation to show in the list
   */
  private loadLastMessagesForConversations(): void {
    if (!this.currentUser?.userId) return;
    
    console.log('[LAST MESSAGES] Loading last message for each conversation');
    
    this.conversations.forEach(conversation => {
      this.messageService.getMessages(this.currentUser!.userId!, conversation.participantId)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (messages: MessageDto[]) => {
            if (messages && messages.length > 0) {
              // Get the last message
              const lastMessage = messages[messages.length - 1];
              conversation.lastMessage = lastMessage.messageText;
              conversation.lastMessageTime = new Date();
              
              // Count unread messages (messages received from this participant that are not read)
              // Note: Jackson serializes isRead as 'read' in JSON
              const unreadCount = messages.filter(msg => 
                msg.senderId === conversation.participantId && 
                msg.receiverId === this.currentUser!.userId &&
                !(msg.read || msg.isRead) // Check both for backwards compatibility
              ).length;
              
              // If this conversation is currently selected, unread count should be 0
              conversation.unreadCount = (this.selectedConversation?.participantId === conversation.participantId) ? 0 : unreadCount;
              console.log(`[LAST MESSAGES] ${conversation.participantName}: "${lastMessage.messageText}", unread: ${conversation.unreadCount}${this.selectedConversation?.participantId === conversation.participantId ? ' (current conversation - reset to 0)' : ''}`);
              
              // Sort after updating
              this.sortConversations();
            }
          },
          error: (err: any) => {
            console.error(`[LAST MESSAGES] Error loading messages for ${conversation.participantName}:`, err);
          }
        });
    });
  }

  // Backwards-compatible alias for templates using the old method name
  public loadConversations(): void {
    this.loadAllUsers();
  }

  selectConversation(conversation: Conversation): void {
    console.log('[SELECT CONVERSATION] Switching to conversation with:', conversation.participantName);
    
    // Clear active status from previous conversation
    if (this.selectedConversation && this.currentUser?.userId && this.wsConnected) {
      this.websocketService.setChatActive(
        this.currentUser.userId,
        this.selectedConversation.participantId,
        false
      );
    }
    
    this.selectedConversation = conversation;
    
    // Reset unread count when opening conversation
    const previousUnreadCount = conversation.unreadCount || 0;
    conversation.unreadCount = 0;
    console.log('[SELECT CONVERSATION] Reset unread count (was:', previousUnreadCount, ')');
    
    // Set active status for new conversation
    if (this.currentUser?.userId && this.wsConnected) {
      this.websocketService.setChatActive(
        this.currentUser.userId,
        conversation.participantId,
        true
      );
      console.log(`[SELECT CONVERSATION] 💬 Set chat active with peer ${conversation.participantId}`);
    }
    
    // Load messages for this conversation
    this.loadMessages();
  }

  loadMessages(): void {
    if (!this.selectedConversation || !this.currentUser) return;

    // Get current user ID
    const userId = this.currentUser.userId || 1;
    
    // Load message history from HTTP (fallback for offline messages)
    this.messageService.getMessages(userId, this.selectedConversation.participantId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (messageDtos: any) => {
          this.messages = messageDtos.map((dto: MessageDto) => {
            // Determine sender name based on senderId
            let senderName = '';
            if (dto.senderId === this.currentUser?.userId) {
              // Message sent by current user
              senderName = `${this.currentUser.firstName} ${this.currentUser.lastName}`;
            } else {
              // Message sent by conversation participant
              senderName = this.selectedConversation?.participantName || 'Unknown';
            }
            
            const message = this.messageService.convertDtoToMessage(dto, senderName);
            
            // Apply cached status if available (this overrides DB status with latest real-time status)
            if (message.id && this.messageStatusCache.has(message.id)) {
              const cachedStatus = this.messageStatusCache.get(message.id)!;
              console.log(`[LOAD MESSAGES] Applying cached status for message ${message.id}: ${cachedStatus}`);
              message.status = cachedStatus;
            }
            
            return message;
          });
          this.shouldScroll = true;
          console.log(`[LOAD MESSAGES] Loaded ${this.messages.length} messages for conversation`);
        },
        error: (err: any) => {
          console.error('Error loading messages:', err);
          this.messages = [];
        }
      });
  }

  sendMessage(): void {
    if (!this.messageText.trim() || !this.currentUser || !this.selectedConversation) {
      return;
    }

    if (!this.wsConnected) {
      console.error('[SEND] ❌ Cannot send - WebSocket not connected');
      alert('WebSocket not connected. Please refresh the page.');
      return;
    }

    const userId = this.currentUser.userId || 1;
    const messageContent = this.messageText;
    const timestamp = new Date();
    
    console.log('[SEND] 📤 Sending message:', messageContent);
    
    // Send message via WebSocket - backend will:
    // 1. Save to DB with messageId
    // 2. Push to BOTH sender and receiver via /queue/messages
    // 3. Send delivery/read status updates via /queue/status
    const wsMessage: WebSocketMessage = {
      messageText: messageContent,
      senderId: userId,
      receiverId: this.selectedConversation.participantId,
      isDelivered: false,
      isRead: false,
      timestamp: timestamp
    };

    this.websocketService.sendMessage(wsMessage);
    
    // OPTIMISTIC UI UPDATE: Add message to UI immediately for better UX
    // Will be confirmed when WebSocket echo arrives
    const optimisticMessage: Message = {
      id: 0, // Temporary ID, will be replaced with real messageId from server
      senderId: this.currentUser!.userId!,
      senderName: `${this.currentUser!.firstName} ${this.currentUser!.lastName}`,
      receiverId: this.selectedConversation.participantId,
      content: wsMessage.messageText,
      timestamp: timestamp,
      status: 'sent' // Start as sent, will update based on server response
    };
    
    this.messages.push(optimisticMessage);
    this.shouldScroll = true;
    console.log('[SEND] ✅ Message added optimistically to UI, waiting for server confirmation');
    
    // Clear input immediately for better UX
    this.messageText = '';
    
    console.log('[SEND] ✅ Message sent to server');
  }

  markMessagesAsRead(): void {
    if (!this.currentUser || !this.selectedConversation) return;

    const userId = this.currentUser.userId || 1;
    this.messageService.markMessagesAsRead(userId, this.selectedConversation.participantId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        error: (err: any) => console.error('Error marking messages as read:', err)
      });
  }

  getStatusIcon(status: string | undefined): string {
    console.log('[STATUS ICON] Getting icon for status:', status);
    if (!status) {
      console.warn('Message has no status, defaulting to sent');
      return 'fa-check';
    }
    switch (status) {
      case 'sent':
        console.log('[STATUS ICON] Returning single check');
        return 'fa-check';
      case 'delivered':
        console.log('[STATUS ICON] Returning double check (delivered)');
        return 'fa-check-double';
      case 'read':
        console.log('[STATUS ICON] Returning double check (read)');
        return 'fa-check-double';
      default:
        console.warn('Unknown status:', status);
        return 'fa-check';
    }
  }

  getStatusClass(status: string | undefined): string {
    if (!status) return 'text-muted';
    return status === 'read' ? 'text-primary' : 'text-muted';
  }

  getStatusTitle(status: string | undefined): string {
    if (!status) return 'Sent';
    switch (status) {
      case 'sent':
        return 'Sent';
      case 'delivered':
        return 'Delivered';
      case 'read':
        return 'Read';
      default:
        return 'Sent';
    }
  }

  isSender(message: Message): boolean {
    return message.senderId === this.currentUser?.userId;
  }

  isUserOnline(userId: number): boolean {
    return this.onlineUsers.has(userId);
  }

  scrollToBottom(): void {
    try {
      this.messagesContainer.nativeElement.scrollTop =
        this.messagesContainer.nativeElement.scrollHeight;
    } catch (err) {}
  }

  onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getUserDisplayName(): string {
    if (this.currentUser) {
      return `${this.currentUser.firstName} ${this.currentUser.lastName}`;
    }
    return 'User';
  }

  getUserInitials(): string {
    if (!this.currentUser) return 'U';
    const first = this.currentUser.firstName?.charAt(0) || '';
    const last = this.currentUser.lastName?.charAt(0) || '';
    return (first + last).toUpperCase() || 'U';
  }

  getConversationInitials(name: string): string {
    if (!name) return '?';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
    }
    return name.charAt(0).toUpperCase();
  }

  formatTime(date: Date | undefined): string {
    if (!date) return '';
    const now = new Date();
    const msgDate = new Date(date);
    const diff = now.getTime() - msgDate.getTime();
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (days === 0) {
      // Today - show time
      return msgDate.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true });
    } else if (days === 1) {
      return 'Yesterday';
    } else if (days < 7) {
      return msgDate.toLocaleDateString('en-US', { weekday: 'short' });
    } else {
      return msgDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    }
  }

  formatMessageTime(date: Date): string {
    return new Date(date).toLocaleTimeString('en-US', { 
      hour: 'numeric', 
      minute: '2-digit', 
      hour12: true 
    });
  }

  onEnterPress(event: KeyboardEvent): void {
    if (!event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
}
