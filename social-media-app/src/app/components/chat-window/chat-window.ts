import {
  Component,
  Input,
  Output,
  EventEmitter,
  ViewChild,
  ElementRef,
  AfterViewChecked,
  OnInit,
  OnDestroy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { FaIconLibrary, FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { fas } from '@fortawesome/free-solid-svg-icons';
import { FirebaseChatService, ChatMessage } from '../../services/firebase-chat.service';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [CommonModule, FormsModule, FontAwesomeModule],
  templateUrl: './chat-window.html',
  styleUrls: ['./chat-window.css']
})
export class ChatWindowComponent implements OnInit, AfterViewChecked, OnDestroy {

  @Input() userName: string = 'Người dùng';
  @Input() userAvatar: string = '';
  @Input() isOnline: boolean = true;
  @Input() conversationId: string = '';  // Conversation ID from Firestore

  @Output() onClose = new EventEmitter<void>();
  @Output() onVideoCall = new EventEmitter<void>();

  messages: ChatMessage[] = [];
  newMessage: string = '';
  isMinimized: boolean = false;
  isClosed: boolean = false;
  isLoading: boolean = false;
  isSending: boolean = false;
  uploadProgress: number = 0;
  uploadingFile: boolean = false;

  @ViewChild('chatBody') private chatBody!: ElementRef;
  @ViewChild('fileInput') private fileInput!: ElementRef;
  @ViewChild('imageInput') private imageInput!: ElementRef;

  private destroy$ = new Subject<void>();

  constructor(
    library: FaIconLibrary,
    private chatService: FirebaseChatService,
    private http: HttpClient
  ) {
    library.addIconPacks(fas);
  }

  ngOnInit() {
    // Subscribe đến danh sách tin nhắn từ Firestore (real-time)
    this.chatService.messages$
      .pipe(takeUntil(this.destroy$))
      .subscribe(messages => {
        this.messages = messages;
        this.scrollToBottom();
      });

    // Subscribe đến trạng thái loading
    this.chatService.isLoading$
      .pipe(takeUntil(this.destroy$))
      .subscribe(isLoading => {
        this.isLoading = isLoading;
      });

    // Tải tin nhắn cho cuộc hội thoại này
    if (this.conversationId) {
      this.chatService.loadMessages(this.conversationId);
      // Mark as read when conversation is opened
      this.chatService.markAsRead(this.conversationId).catch(err =>
        console.warn('Could not mark as read:', err)
      );
    }
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  ngOnDestroy() {
    // Hủy đăng ký Firebase listener
    if (this.conversationId) {
      this.chatService.unsubscribeFromMessages(this.conversationId);
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Send text message via backend API
   * Backend will write to Firestore and update conversation metadata
   */
  async sendMessage() {
    if (!this.newMessage.trim() || !this.conversationId || this.isSending) return;

    this.isSending = true;
    try {
      await this.chatService.sendMessage(this.conversationId, this.newMessage);
      this.newMessage = '';
    } catch (error) {
      console.error('Error sending message:', error);
      alert('Lỗi: Không thể gửi tin nhắn');
    } finally {
      this.isSending = false;
    }
  }

  triggerFileInput() {
    this.fileInput.nativeElement.click();
  }

  triggerImageInput() {
    this.imageInput.nativeElement.click();
  }

  /**
   * Upload file and send as message
   * Step 1: Upload file to backend (/api/firestore/chat/upload-file)
   * Step 2: Send message with file URL
   */
  async onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file && this.conversationId) {
      this.uploadingFile = true;
      this.isSending = true;

      try {
        await this.chatService.sendFile(this.conversationId, file, 'document');
      } catch (error) {
        console.error('Error sending file:', error);
        alert('Lỗi: Không thể gửi file');
      } finally {
        this.uploadingFile = false;
        this.isSending = false;
        // Reset input
        event.target.value = '';
      }
    }
  }

  /**
   * Upload image and send as message
   * Same flow as file upload
   */
  async onImageSelected(event: any) {
    const file = event.target.files[0];
    if (file && this.conversationId) {
      this.uploadingFile = true;
      this.isSending = true;

      try {
        await this.chatService.sendFile(this.conversationId, file, 'image');
      } catch (error) {
        console.error('Error sending image:', error);
        alert('Lỗi: Không thể gửi hình ảnh');
      } finally {
        this.uploadingFile = false;
        this.isSending = false;
        // Reset input
        event.target.value = '';
      }
    }
  }

  startVideoCall() {
    this.onVideoCall.emit();
    // TODO: Mở modal video call hoặc tích hợp WebRTC / Daily.co / Twilio...
  }

  toggleMinimize() {
    this.isMinimized = !this.isMinimized;
  }

  closeChat() {
    this.isClosed = true;
    this.onClose.emit();
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.chatBody) {
        this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight;
      }
    });
  }
}