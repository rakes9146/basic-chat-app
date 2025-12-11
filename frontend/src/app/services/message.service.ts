import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { Message, Conversation, MessageDto } from '../models/chat.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private messageServiceUrl = `${environment.messageServiceUrl}`;
  private messagesSubject = new BehaviorSubject<Message[]>([]);
  public messages$ = this.messagesSubject.asObservable();
  private conversationsSubject = new BehaviorSubject<Conversation[]>([]);
  public conversations$ = this.conversationsSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Get messages between two users
  getMessages(senderId: number, receiverId: number): Observable<MessageDto[]> {
    const params = new HttpParams()
      .set('senderId', senderId.toString())
      .set('receiverId', receiverId.toString());
    
    return this.http.get<MessageDto[]>(`${this.messageServiceUrl}`, { params });
  }

  // Send a new message
  sendMessage(messageDto: MessageDto): Observable<MessageDto> {
    return this.http.post<MessageDto>(`${this.messageServiceUrl}`, messageDto);
  }

  // Update message delivery status
  updateDeliveryStatus(messageId: number): Observable<any> {
    return this.http.put<any>(`${this.messageServiceUrl}/${messageId}/delivery`, {});
  }

  // Update message read status
  updateReadStatus(messageId: number): Observable<any> {
    return this.http.put<any>(`${this.messageServiceUrl}/${messageId}/read`, {});
  }

  // Mark all messages between users as read
  markMessagesAsRead(senderId: number, receiverId: number): Observable<any> {
    const params = new HttpParams()
      .set('senderId', senderId.toString())
      .set('receiverId', receiverId.toString());
    
    return this.http.get<any>(`${this.messageServiceUrl}/markAsRead`, { params });
  }

  updateConversations(conversations: Conversation[]): void {
    this.conversationsSubject.next(conversations);
  }

  updateMessages(messages: Message[]): void {
    this.messagesSubject.next(messages);
  }

  addMessage(message: Message): void {
    const currentMessages = this.messagesSubject.value;
    this.messagesSubject.next([...currentMessages, message]);
  }

  // Convert MessageDto to Message for UI
  convertDtoToMessage(dto: MessageDto, senderName: string): Message {
    let status: 'sent' | 'delivered' | 'read' = 'sent';
    // Jackson serializes boolean isRead/isDelivered as read/delivered in JSON
    if (dto.read || dto.isRead) {
      status = 'read';
    } else if (dto.delivered || dto.isDelivered) {
      status = 'delivered';
    }

    // Use createdDate from database, fallback to timestamp or current date
    let messageTimestamp: Date;
    if (dto.createdDate) {
      messageTimestamp = new Date(dto.createdDate);
    } else if (dto.timestamp) {
      messageTimestamp = new Date(dto.timestamp);
    } else {
      messageTimestamp = new Date();
    }

    return {
      id: dto.messageId,
      senderId: dto.senderId,
      senderName: senderName,
      receiverId: dto.receiverId,
      content: dto.messageText,
      timestamp: messageTimestamp,
      status: status
    };
  }

  // Convert Message to MessageDto for API
  convertMessageToDto(message: Message): MessageDto {
    return {
      messageId: message.id,
      messageText: message.content,
      senderId: message.senderId,
      receiverId: message.receiverId,
      isDelivered: message.status === 'delivered' || message.status === 'read',
      isRead: message.status === 'read'
    };
  }
}
