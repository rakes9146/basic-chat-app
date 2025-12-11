export interface User {
  firstName: string;
  lastName: string;
  email: string;
  userName: string;
  password?: string;
  userId?: number;
}

export interface MessageDto {
  messageId?: number;
  messageText: string;
  senderId: number;
  receiverId: number;
  // Jackson serializes boolean fields isDelivered/isRead as delivered/read
  delivered?: boolean;
  read?: boolean;
  // Keep these for backwards compatibility
  isDelivered?: boolean;
  isRead?: boolean;
  createdDate?: string | Date; // LocalDateTime from backend
  timestamp?: Date; // Deprecated, use createdDate
}

export interface Message {
  id?: number;
  senderId: number;
  senderName: string;
  receiverId: number;
  content: string;
  timestamp: Date;
  status: 'sent' | 'delivered' | 'read';
  messageType?: 'text' | 'image' | 'file';
}

export interface Conversation {
  id?: number;
  participantId: number;
  participantName: string;
  lastMessage?: string;
  lastMessageTime?: Date;
  unreadCount: number;
  messages: Message[];
}

export interface AuthResponse {
  success: boolean;
  message: string;
  user?: User;
  token?: string;
}

