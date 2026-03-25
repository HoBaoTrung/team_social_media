import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../../environments/environment";
import { Page } from "../interfaces/page.model";

export interface NotificationDTO {
  id: number;               
  notificationType: string;
  createdAt: string;      
  referenceId: number;
  referenceType: string;
  isRead: boolean;
  sender: {
    id: number;
    username: string;
    avatarUrl: string | null;
    fullName: string | null;
  };
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor(private http: HttpClient) {}

  getNotifications(page: number = 0, size: number = 10): Observable<Page<NotificationDTO>> {
    return this.http.get<Page<NotificationDTO>>(
      `${environment.apiUrl}/notifications?page=${page}&size=${size}`
    );
  }

  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${environment.apiUrl}/notifications/unread-count`);
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${environment.apiUrl}/notifications/${id}/read`, {});
  }

  markAllAsRead(): Observable<{ updated: number }> {
    return this.http.patch<{ updated: number }>(`${environment.apiUrl}/notifications/read-all`, {});
  }
}