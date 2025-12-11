import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { Message, Conversation } from '../models/chat.model';

@Injectable({
  providedIn: 'root'
})
export class MessageService {
  private apiUrl = 'http://localhost:8080/api/messages';
  private messagesSubject = new BehaviorSubject<Message[]>([]);
  public messages$ = this.messagesSubject.asObservable();
  private conversationsSubject = new BehaviorSubject<Conversation[]>([]);
  public conversations$ = this.conversationsSubject.asObservable();

  constructor(private http: HttpClient) {}

  getMessages(userId: number, participantId: number): Observable<Message[]> {
    return this.http.get<Message[]>(`${this.apiUrl}/conversation/${userId}/${participantId}`);
  }

  getConversations(userId: number): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(`${this.apiUrl}/conversations/${userId}`);
  }

  sendMessage(message: Message): Observable<Message> {
    return this.http.post<Message>(`${this.apiUrl}/send`, message);
  }

  updateMessageStatus(messageId: number, status: 'delivered' | 'read'): Observable<Message> {
    return this.http.put<Message>(`${this.apiUrl}/${messageId}/status`, { status });
  }

  markMessagesAsRead(userId: number, participantId: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/markAsRead`, { userId, participantId });
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
}
