import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBell } from '@fortawesome/free-solid-svg-icons';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { RouterModule } from '@angular/router';

import { NotificationService, NotificationDTO } from '../../services/notification.service';
import { DropdownBaseComponent } from '../shared/dropdown-base.component';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-notifications-dropdown',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, ScrollingModule, RouterModule],
  templateUrl: './notifications-dropdown.html',
  styleUrl: './notifications-dropdown.css'
})
export class NotificationsDropdown extends DropdownBaseComponent implements OnInit {

  faBell = faBell;
  notifications = signal<NotificationDTO[]>([]);

  constructor(private notificationService: NotificationService) {
    super();
  }

  ngOnInit() {
    this.loadUnreadCount();
    this.startPolling();
  }

  // TrackBy function
  trackById = (index: number, notif: NotificationDTO): number => notif.id;


  private loadUnreadCount() {
    this.notificationService.getUnreadCount().subscribe({
      next: (res) => this.unreadCount = res.count,
      error: () => this.unreadCount = 0
    });
  }

  
  override loadInitialData() {
    this.loadMoreData();
  }


  override loadMoreData() {
    if (this.isLoadingMore || !this.hasMore) {
      return;
    }

    this.isLoadingMore = true;

    this.notificationService.getNotifications(this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        const currentList = this.notifications();

        if (this.isFirstLoad) {
          this.notifications.set(page.content);
          this.isFirstLoad = false;
        } else {
          this.notifications.set([...currentList, ...page.content]);
        }

        this.hasMore = !page.last;
        this.currentPage++;
        this.isLoadingMore = false;
      },
      error: (err) => {
        console.error('Load notifications failed:', err);
        this.errorMessage = 'Không tải được thông báo';
        this.isLoadingMore = false;
      }
    });
  }

 
  override fetchUnreadCount(): Observable<{ count: number }> {
    return this.notificationService.getUnreadCount();
  }

 
  override getDropdownSelector(): string {
    return '.notification-dropdown';
  }


  override getTriggerSelector(): string {
    return '.notification-trigger';
  }


  markAsRead(notification: NotificationDTO) {
    if (notification.isRead) return;

    this.notificationService.markAsRead(notification.id).subscribe({
      next: () => {
        this.notifications.update(list =>
          list.map(n => n.id === notification.id ? { ...n, isRead: true } : n)
        );
        this.unreadCount = Math.max(0, this.unreadCount - 1);
      }
    });
  }

 
  markAllAsRead() {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.update(list => list.map(n => ({ ...n, isRead: true })));
        this.unreadCount = 0;
      }
    });
  }
}