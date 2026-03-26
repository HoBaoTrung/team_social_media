import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../../environments/environment";
import { Page } from "../interfaces/page.model";

export interface ParticipantDto {
  id: number;       
  username: string;
  fullName: string | null;
  avatar: string | null;
  role?: string;
  online: boolean;
}

export interface ConversationDto {       
  id: string;                              
  name: string | null;
  avatar: string | null;
  type: 'private' | 'group';
  participantCount?: number;
  participants?: ParticipantDto[];        

  lastMessage: string | null;             
  timeAgo: string | null;
  isOnline: boolean;
  hasUnread: boolean;
  unreadCount: number;
  lastMessageTime?: string;
}

//  giữ cấu trúc cũ để dễ bind template
export interface ConversationViewModel {
  id: string;
  name: string;
  avatar: string | null;
  type: 'private' | 'group';
  unreadCount: number;
  lastMessagePreview: string | null;
  timeAgo: string | null;
  isOnline: boolean;
  participants?: ParticipantDto[];
}

@Injectable({
  providedIn: 'root'
})
export class MessageService {

  constructor(private http: HttpClient) {}

  getConversations(page: number = 0, size: number = 20): Observable<Page<ConversationDto>> {
    return this.http.get<Page<ConversationDto>>(
      `${environment.apiUrl}/conversations?page=${page}&size=${size}`   // ← thêm /api/ nếu cần
    );
  }

  getUnreadCount(userId: number): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${environment.apiUrl}/chat/unread/total/${userId}`);
  }

  markAsRead(conversationId: string | number, userId: number): Observable<void> {   // id có thể là string
    return this.http.patch<void>(
      `${environment.apiUrl}/chat/mark-read/${conversationId}/${userId}`, 
      {}
    );
  }
}
