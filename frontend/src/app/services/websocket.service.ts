import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject, Observable } from 'rxjs';
import { Client, IFrame, Message } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { environment } from '../../environments/environment';

export interface WebSocketMessage {
  messageId?: number;
  messageText: string;
  senderId: number;
  receiverId: number;
  isDelivered: boolean;
  isRead: boolean;
  timestamp?: Date;
}

export interface MessageStatus {
  messageId: number;
  senderId: number;
  receiverId: number;
  delivered?: boolean;
  read?: boolean;
  deliveredAt?: Date;
  readAt?: Date;
}

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private client: Client;
  private wsUrl = environment.webSocketUrl;
  private userId: number | null = null;
  
  public isConnected$ = new BehaviorSubject<boolean>(false);
  public messages$ = new Subject<WebSocketMessage>();
  public deliveryStatus$ = new Subject<MessageStatus>();
  public readStatus$ = new Subject<MessageStatus>();
  public presenceUpdates$ = new Subject<{userId: number, online: boolean}>();
  public connectionError$ = new Subject<string>();

  constructor() {
    // Client will be initialized in connect() with userId
    this.client = null as any;
  }

  /**
   * Connect to WebSocket server with userId
   */
  public connect(userId?: number): void {
    if (this.client && this.client.active) {
      console.log('WebSocket already connected');
      return;
    }

    // Store userId for reconnection and subscriptions
    if (userId !== undefined) {
      this.userId = userId;
    }

    if (this.userId === null) {
      console.error('Cannot connect without userId');
      return;
    }

    console.log('[WEBSOCKET] Connecting with userId:', this.userId);

    // Create WebSocket URL with userId as query parameter
    const wsUrlWithUserId = `${this.wsUrl}?userId=${this.userId}`;
    console.log('[WEBSOCKET] Connection URL:', wsUrlWithUserId);

    // Initialize client with userId in both URL and headers
    this.client = new Client({
      webSocketFactory: () => {
        console.log('[WEBSOCKET] Creating SockJS with URL:', wsUrlWithUserId);
        return new SockJS(wsUrlWithUserId);
      },
      connectHeaders: {
        'userId': this.userId.toString()
      },
      onConnect: () => this.onConnect(),
      onDisconnect: () => this.onDisconnect(),
      onStompError: (frame: IFrame) => this.onStompError(frame),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => console.log('[STOMP]:', str)
    });

    this.client.activate();
  }

  /**
   * Disconnect from WebSocket server
   */
  public disconnect(): void {
    if (this.client && this.client.active) {
      this.client.deactivate();
    }
  }

  /**
   * Send message via WebSocket
   */
  public sendMessage(message: WebSocketMessage): void {
    if (!this.client || !this.client.active) {
      console.error('WebSocket not connected');
      this.connectionError$.next('WebSocket not connected');
      return;
    }

    this.client.publish({
      destination: '/app/send',
      body: JSON.stringify(message),
      headers: { 'content-type': 'application/json' }
    });

    console.log('Message sent via WebSocket:', message);
  }

  /**
   * Mark message as delivered
   */
  public markMessageDelivered(messageId: number, receiverId: number): void {
    if (!this.client || !this.client.active) {
      console.error('WebSocket not connected');
      return;
    }

    const dto: any = {
      messageId: messageId,
      receiverId: receiverId
    };

    this.client.publish({
      destination: '/app/deliver',
      body: JSON.stringify(dto),
      headers: { 'content-type': 'application/json' }
    });

    console.log('Marked message as delivered:', messageId);
  }

  /**
   * Mark message as read
   */
  public markMessageRead(messageId: number, receiverId: number): void {
    if (!this.client || !this.client.active) {
      console.error('WebSocket not connected');
      return;
    }

    const dto: any = {
      messageId: messageId,
      receiverId: receiverId
    };

    this.client.publish({
      destination: '/app/read',
      body: JSON.stringify(dto),
      headers: { 'content-type': 'application/json' }
    });

    console.log('Marked message as read:', messageId);
  }

  /**
   * Announce user presence (online/offline)
   */
  public announcePresence(userId: number, online: boolean): void {
    if (!this.client || !this.client.active) {
      console.error('WebSocket not connected - cannot announce presence');
      return;
    }

    const presenceDto = {
      userId: userId,
      online: online
    };

    this.client.publish({
      destination: '/app/presence',
      body: JSON.stringify(presenceDto),
      headers: { 'content-type': 'application/json' }
    });

    console.log(`[WEBSOCKET] 🟢 Presence announced: User ${userId} is ${online ? 'online' : 'offline'}`);
  }

  /**
   * Announce chat activity (user actively viewing a specific chat)
   */
  public setChatActive(userId: number, peerId: number, active: boolean): void {
    if (!this.client || !this.client.active) {
      console.error('WebSocket not connected - cannot set chat active');
      return;
    }

    const chatActivityDto = {
      userId: userId,
      peerId: peerId,
      active: active
    };

    this.client.publish({
      destination: '/app/chat/active',
      body: JSON.stringify(chatActivityDto),
      headers: { 'content-type': 'application/json' }
    });

    console.log(`[WEBSOCKET] 💬 Chat activity: User ${userId} is ${active ? 'active' : 'inactive'} in chat with peer ${peerId}`);
  }

  /**
   * Subscribe to incoming messages for current user
   */
  public subscribeToMessages(userId: number): Subscription {
    if (!this.client || !this.client.active) {
      console.error('[WEBSOCKET] Cannot subscribe - WebSocket not connected');
      console.error('[WEBSOCKET] Client exists:', !!this.client);
      console.error('[WEBSOCKET] Client active:', this.client?.active);
      return null;
    }

    const destination = `/user/${userId}/queue/messages`;
    console.log(`[WEBSOCKET] ==========================================`);
    console.log(`[WEBSOCKET] 📬 SUBSCRIBING TO MESSAGES`);
    console.log(`[WEBSOCKET] Destination: ${destination}`);
    console.log(`[WEBSOCKET] UserId: ${userId}`);
    console.log(`[WEBSOCKET] Client connected: ${this.client.connected}`);
    console.log(`[WEBSOCKET] ==========================================`);
    
    const subscription = this.client.subscribe(destination, (message: Message) => {
      console.log('[WEBSOCKET] ==========================================');
      console.log('[WEBSOCKET] 🎉 MESSAGE CALLBACK TRIGGERED!!!');
      console.log('[WEBSOCKET] ==========================================');
      console.log('[WEBSOCKET] Raw message received:', message);
      console.log('[WEBSOCKET] Message body type:', typeof message.body);
      console.log('[WEBSOCKET] Message body length:', message.body?.length);
      console.log('[WEBSOCKET] Message body:', message.body);
      
      try {
        const msg = JSON.parse(message.body);
        console.log('[WEBSOCKET] ✅ Parsed message successfully:', msg);
        console.log('[WEBSOCKET] Message fields:', Object.keys(msg));
        console.log('[WEBSOCKET] Emitting message to messages$ subject');
        
        this.messages$.next(msg);
        console.log('[WEBSOCKET] ✅ Message emitted to messages$ successfully');
      } catch (error) {
        console.error('[WEBSOCKET] ❌ Error parsing message:', error);
        console.error('[WEBSOCKET] Raw body was:', message.body);
      }
    });
    
    console.log('[WEBSOCKET] Subscription created:', subscription);
    console.log('[WEBSOCKET] Subscription ID:', subscription?.id);
    
    return subscription;
  }

  /**
   * Subscribe to delivery status updates
   */
  public subscribeToDeliveryStatus(userId: number): Subscription {
    if (!this.client || !this.client.active) {
      console.error('WebSocket not connected');
      return null;
    }

    console.log(`Subscribing to delivery status for user: ${userId}`);
    
    return this.client.subscribe(`/user/${userId}/queue/status`, (message: Message) => {
      try {
        const status = JSON.parse(message.body);
        console.log('[STATUS] Received status update:', status);
        
        // Check if it's a delivery status (has 'delivered' field)
        if (status.delivered !== undefined && status.delivered === true) {
          console.log('[STATUS] ✅ Delivery status detected for message:', status.messageId);
          this.deliveryStatus$.next(status);
        } 
        // Check if it's a read status (has 'read' field)
        else if (status.read !== undefined && status.read === true) {
          console.log('[STATUS] ✅ Read status detected for message:', status.messageId);
          this.readStatus$.next(status);
        } else {
          console.log('[STATUS] ⚠️ Unknown status format:', status);
        }
      } catch (error) {
        console.error('Error parsing status:', error);
        console.error('Raw message body:', message.body);
      }
    });
  }

  /**
   * Subscribe to user presence updates (online/offline)
   */
  public subscribeToPresence(): Subscription {
    if (!this.client || !this.client.active) {
      console.error('[PRESENCE] WebSocket not connected');
      return null;
    }

    console.log('[PRESENCE] 📡 Subscribing to /topic/presence');
    
    return this.client.subscribe('/topic/presence', (message: Message) => {
      try {
        const presence = JSON.parse(message.body);
        console.log('[PRESENCE] 👤 Received:', presence);
        this.presenceUpdates$.next(presence);
      } catch (error) {
        console.error('[PRESENCE] ❌ Error parsing presence:', error);
      }
    });
  }

  /**
   * Subscribe to initial presence list (online users when you connect)
   */
  public subscribeToPresenceInit(userId: number): Subscription {
    if (!this.client || !this.client.active) {
      console.error('[PRESENCE-INIT] WebSocket not connected');
      return null;
    }

    console.log('[PRESENCE-INIT] 📡 Subscribing to /user/' + userId + '/queue/presence-init');
    
    return this.client.subscribe(`/user/${userId}/queue/presence-init`, (message: Message) => {
      try {
        const presence = JSON.parse(message.body);
        console.log('[PRESENCE-INIT] 👥 Received online user:', presence);
        this.presenceUpdates$.next(presence);
      } catch (error) {
        console.error('[PRESENCE-INIT] ❌ Error parsing presence:', error);
      }
    });
  }

  /**
   * Private method: Called when WebSocket connects
   */
  private onConnect(): void {
    console.log('[WEBSOCKET] ✅ Connected successfully to:', this.wsUrl);
    if (this.client) {
      console.log('[WEBSOCKET] Client active:', this.client.active);
      console.log('[WEBSOCKET] Client connected:', this.client.connected);
    }
    this.isConnected$.next(true);
  }

  /**
   * Private method: Called when WebSocket disconnects
   */
  private onDisconnect(): void {
    console.log('[WEBSOCKET] ❌ Disconnected from:', this.wsUrl);
    this.isConnected$.next(false);
  }

  /**
   * Private method: Called on STOMP error
   */
  private onStompError(frame: IFrame): void {
    const errorMessage = `Stomp error: ${frame.headers['message']} (${frame.body})`;
    console.error('[WEBSOCKET] 🔴 STOMP Error:', errorMessage);
    console.error('[WEBSOCKET] Frame details:', frame);
    this.connectionError$.next(errorMessage);
  }

  /**
   * Check if WebSocket is connected
   */
  public isConnected(): boolean {
    return this.client ? this.client.active : false;
  }
}

// Subscription type alias for return value
export type Subscription = any;
