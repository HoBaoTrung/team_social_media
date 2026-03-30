import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import {
  getFirestore,
  collection,
  doc,
  onSnapshot,
  query,
  orderBy,
  limitToLast,
  getDocs,
  Query
} from 'firebase/firestore';
import { firestore } from '../../config/FireBaseConfig';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export interface ChatMessage {
  id?: string;
  senderId: number;
  senderName: string;
  senderAvatar: string;
  receiverId: number | string;
  content: string;
  type: 'TEXT' | 'IMAGE' | 'FILE';
  timestamp: number;
  isMine?: boolean;
  fileName?: string;
  fileUrl?: string;
  time?: string;
  readAt?: number;
}

export interface Conversation {
  id?: string;
  participants: { [userId: string]: boolean };
  participantNames?: string[];
  lastMessage?: string;
  lastMessageTime?: number;
  updatedAt?: number;
  unreadCount?: { [userId: string]: number };
  lastRead?: { [userId: string]: number };
}

@Injectable({
  providedIn: 'root'
})
export class FirebaseChatService {
  private messagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  public messages$ = this.messagesSubject.asObservable();

  private conversationsSubject = new BehaviorSubject<Conversation[]>([]);
  public conversations$ = this.conversationsSubject.asObservable();

  private isLoadingSubject = new BehaviorSubject<boolean>(false);
  public isLoading$ = this.isLoadingSubject.asObservable();

  private currentUserId: number | null = null;
  private messageUnsubscribers: Map<string, () => void> = new Map();
  private conversationUnsubscriber: (() => void) | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.currentUserId = user.id;
    }
  }

  /**
   * Load all conversations for current user (real-time listener)
   */
  loadConversations(): void {
    if (!this.currentUserId) return;

    this.isLoadingSubject.next(true);

    try {
      const conversationsRef = collection(firestore, 'conversations');
      const userConversationsRef = doc(firestore, 'conversations', String(this.currentUserId));

      // Unsubscribe from previous listener
      if (this.conversationUnsubscriber) {
        this.conversationUnsubscriber();
      }

      // Subscribe to user's conversations subcollection
      // Note: Firestore Security Rules will filter this
      this.conversationUnsubscriber = onSnapshot(
        collection(userConversationsRef, 'userConversations'),
        (snapshot) => {
          const conversations: Conversation[] = [];
          snapshot.forEach((doc) => {
            conversations.push({
              id: doc.id,
              ...doc.data()
            } as Conversation);
          });

          // Sort by updatedAt descending
          conversations.sort((a, b) =>
            (b.updatedAt || 0) - (a.updatedAt || 0)
          );

          this.conversationsSubject.next(conversations);
          this.isLoadingSubject.next(false);
        },
        (error) => {
          console.error('Error loading conversations:', error);
          this.isLoadingSubject.next(false);
        }
      );

    } catch (error) {
      console.error('Error setting up conversations listener:', error);
      this.isLoadingSubject.next(false);
    }
  }

  /**
   * Load messages from a conversation (real-time listener with pagination)
   *
   * @param conversationId Conversation ID to load messages from
   * @param limit Number of messages to load (default 50)
   */
  loadMessages(conversationId: string, limit: number = 50): void {
    if (!this.currentUserId) return;

    this.isLoadingSubject.next(true);

    try {
      const messagesRef = collection(
        firestore,
        'conversations',
        conversationId,
        'messages'
      );

      // Build query: order by timestamp and limit
      const messagesQuery = query(
        messagesRef,
        orderBy('createdAt', 'desc'),
        limitToLast(limit)
      );

      // Unsubscribe from previous listener for this conversation
      if (this.messageUnsubscribers.has(conversationId)) {
        const unsubscribe = this.messageUnsubscribers.get(conversationId);
        if (unsubscribe) unsubscribe();
      }

      // Subscribe to messages real-time
      const unsubscribe = onSnapshot(
        messagesQuery,
        (snapshot) => {
          const messages: ChatMessage[] = [];

          snapshot.forEach((doc) => {
            const data = doc.data();
            messages.push({
              id: doc.id,
              senderId: data['senderId'] as number,
              senderName: data['senderName'] as string || '',
              senderAvatar: data['senderAvatar'] as string || '',
              receiverId: data['receiverId'] as number,
              content: data['content'] as string,
              type: data['type'] as 'TEXT' | 'IMAGE' | 'FILE',
              timestamp: data['createdAt']?.toMillis?.() || 0,
              isMine: data['senderId'] === this.currentUserId,
              fileName: data['fileName'] as string,
              fileUrl: data['fileUrl'] as string,
              readAt: data['readAt']?.toMillis?.() || undefined,
              time: this.formatTime(data['createdAt']?.toMillis?.() || 0)
            } as ChatMessage);
          });

          // Sort by timestamp ascending (oldest first)
          messages.sort((a, b) => a.timestamp - b.timestamp);

          this.messagesSubject.next(messages);
          this.isLoadingSubject.next(false);
        },
        (error) => {
          console.error('Error loading messages:', error);
          this.isLoadingSubject.next(false);
        }
      );

      // Store unsubscriber for later cleanup
      this.messageUnsubscribers.set(conversationId, unsubscribe);

    } catch (error) {
      console.error('Error setting up messages listener:', error);
      this.isLoadingSubject.next(false);
    }
  }

  /**
   * Send a text message via backend API (not direct Firestore write)
   *
   * Backend will:
   * - Validate user permissions
   * - Write to Firestore
   * - Update conversation metadata
   * - Increment unread counts
   *
   * Frontend Firestore listener will auto-update the messages list
   */
  async sendMessage(
    conversationId: string,
    content: string
  ): Promise<void> {
    if (!this.currentUserId) {
      throw new Error('User not authenticated');
    }

    try {
      const response = await this.http.post<{ success: boolean; messageId: string }>(
        `${environment.apiUrl}/firestore/chat/send-message`,
        {
          conversationId,
          content,
          type: 'TEXT',
          receiverId: 0  // Optional, for 1:1 chats
        }
      ).toPromise();

      if (!response || !response.success) {
        throw new Error('Failed to send message');
      }

      console.log('✅ Message sent:', response.messageId);
    } catch (error) {
      console.error('Error sending message:', error);
      throw error;
    }
  }

  /**
   * Upload file and send as message
   *
   * @param conversationId Conversation ID
   * @param file File to upload
   * @param type 'image' or 'document'
   */
  async sendFile(
    conversationId: string,
    file: File,
    type: 'image' | 'document'
  ): Promise<void> {
    if (!this.currentUserId) {
      throw new Error('User not authenticated');
    }

    try {
      // Step 1: Upload file to backend
      const formData = new FormData();
      formData.append('file', file);
      formData.append('type', type);

      const uploadResponse = await this.http.post<{
        success: boolean;
        fileUrl: string;
        fileName: string;
        fileType: string;
      }>(
        `${environment.apiUrl}/firestore/chat/upload-file`,
        formData
      ).toPromise();

      if (!uploadResponse || !uploadResponse.success) {
        throw new Error('Failed to upload file');
      }

      const { fileUrl, fileName, fileType } = uploadResponse;

      // Step 2: Send message with file URL
      const messageType = fileType === 'IMAGE' ? 'IMAGE' : 'FILE';
      const response = await this.http.post<{ success: boolean; messageId: string }>(
        `${environment.apiUrl}/firestore/chat/send-message`,
        {
          conversationId,
          content: `[${fileType}] ${fileName}`,
          type: messageType,
          fileUrl,
          fileName,
          receiverId: 0
        }
      ).toPromise();

      if (!response || !response.success) {
        throw new Error('Failed to send file message');
      }

      console.log('✅ File sent:', fileUrl);
    } catch (error) {
      console.error('Error uploading file:', error);
      throw error;
    }
  }

  /**
   * Mark conversation as read (reset unread count)
   */
  async markAsRead(conversationId: string): Promise<void> {
    if (!this.currentUserId) {
      return;
    }

    try {
      const response = await this.http.put<{ success: boolean }>(
        `${environment.apiUrl}/firestore/chat/mark-read/${conversationId}`,
        {}
      ).toPromise();

      if (response?.success) {
        console.log('✅ Marked as read:', conversationId);
      }
    } catch (error) {
      console.error('Error marking as read:', error);
      // Don't throw - this is non-critical
    }
  }

  /**
   * Get unread count for a conversation
   */
  async getUnreadCount(conversationId: string): Promise<number> {
    if (!this.currentUserId) {
      return 0;
    }

    try {
      const response = await this.http.get<{
        success: boolean;
        unreadCount: number;
      }>(
        `${environment.apiUrl}/firestore/chat/unread-count/${conversationId}`
      ).toPromise();

      return response?.unreadCount || 0;
    } catch (error) {
      console.error('Error getting unread count:', error);
      return 0;
    }
  }

  /**
   * Create a new conversation or get existing one
   */
  async createConversation(participantIds: number[]): Promise<string> {
    if (!this.currentUserId) {
      throw new Error('User not authenticated');
    }

    try {
      const response = await this.http.post<{
        success: boolean;
        conversationId: string;
      }>(
        `${environment.apiUrl}/firestore/chat/create-conversation`,
        { participantIds }
      ).toPromise();

      if (!response || !response.success) {
        throw new Error('Failed to create conversation');
      }

      return response.conversationId;
    } catch (error) {
      console.error('Error creating conversation:', error);
      throw error;
    }
  }

  /**
   * Get current messages
   */
  getCurrentMessages(): ChatMessage[] {
    return this.messagesSubject.value;
  }

  /**
   * Unsubscribe from specific conversation's messages
   */
  unsubscribeFromMessages(conversationId: string): void {
    if (this.messageUnsubscribers.has(conversationId)) {
      const unsubscribe = this.messageUnsubscribers.get(conversationId);
      if (unsubscribe) {
        unsubscribe();
        this.messageUnsubscribers.delete(conversationId);
      }
    }
  }

  /**
   * Cleanup all listeners when service is destroyed
   */
  unsubscribeAll(): void {
    // Unsubscribe from all message listeners
    this.messageUnsubscribers.forEach((unsubscriber) => {
      if (unsubscriber) unsubscriber();
    });
    this.messageUnsubscribers.clear();

    // Unsubscribe from conversations listener
    if (this.conversationUnsubscriber) {
      this.conversationUnsubscriber();
      this.conversationUnsubscriber = null;
    }

    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Format timestamp to readable time
   */
  private formatTime(timestamp: number): string {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
