import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { MessageService } from '../../services/message.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { Message, Conversation, User } from '../../models/chat.model';
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
  loading = false;
  private destroy$ = new Subject<void>();
  private shouldScroll = false;

  constructor(
    private messageService: MessageService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.pipe(takeUntil(this.destroy$)).subscribe(user => {
      this.currentUser = user;
      if (user) {
        this.loadConversations();
      }
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadConversations(): void {
    if (!this.currentUser) return;

    this.loading = true;
    this.messageService.getConversations(this.currentUser.id || 0).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (conversations: any) => {
        this.conversations = conversations;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  selectConversation(conversation: Conversation): void {
    this.selectedConversation = conversation;
    this.loadMessages();
  }

  loadMessages(): void {
    if (!this.selectedConversation || !this.currentUser) return;

    this.messageService.getMessages(
      this.currentUser.id || 0,
      this.selectedConversation.participantId
    ).pipe(takeUntil(this.destroy$)).subscribe({
      next: (messages: any) => {
        this.messages = messages;
        this.shouldScroll = true;
        this.markMessagesAsRead();
      }
    });
  }

  sendMessage(): void {
    if (!this.messageText.trim() || !this.currentUser || !this.selectedConversation) {
      return;
    }

    const newMessage: Message = {
      senderId: this.currentUser.id || 0,
      senderName: `${this.currentUser.firstName} ${this.currentUser.lastName}`,
      receiverId: this.selectedConversation.participantId,
      content: this.messageText,
      timestamp: new Date(),
      status: 'sent'
    };

    this.messageService.sendMessage(newMessage).pipe(takeUntil(this.destroy$)).subscribe({
      next: (message: any) => {
        this.messages.push(message);
        this.messageText = '';
        this.shouldScroll = true;
      }
    });
  }

  markMessagesAsRead(): void {
    if (!this.currentUser || !this.selectedConversation) return;

    this.messageService.markMessagesAsRead(
      this.currentUser.id || 0,
      this.selectedConversation.participantId
    ).pipe(takeUntil(this.destroy$)).subscribe();
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'sent':
        return 'fa-check';
      case 'delivered':
        return 'fa-check-double';
      case 'read':
        return 'fa-check-double';
      default:
        return '';
    }
  }

  getStatusClass(status: string): string {
    return status === 'read' ? 'text-primary' : 'text-muted';
  }

  isSender(message: Message): boolean {
    return message.senderId === this.currentUser?.id;
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
}
