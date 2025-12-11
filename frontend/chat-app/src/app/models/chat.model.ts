export interface User {
  id?: number;
  username: string;
  password: string;
  firstName: string;
  lastName: string;
  email?: string;
  createdAt?: Date;
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
