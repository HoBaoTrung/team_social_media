import { Component, ElementRef, HostListener, ViewChild, OnInit, OnDestroy, signal, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBell } from '@fortawesome/free-solid-svg-icons';
import { ScrollingModule, CdkVirtualScrollViewport } from '@angular/cdk/scrolling';

import { NotificationService, NotificationDTO } from '../../services/notification.service';
import { Subscription, interval, fromEvent } from 'rxjs';
import { switchMap, debounceTime } from 'rxjs/operators';

@Component({
  selector: 'app-notifications-dropdown',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, ScrollingModule],
  templateUrl: './notifications-dropdown.html',
  styleUrl: './notifications-dropdown.css'
})
export class NotificationsDropdown implements OnInit, OnDestroy, AfterViewInit {

  @ViewChild('dropdown') dropdownRef!: ElementRef;
  @ViewChild('trigger') triggerRef!: ElementRef;
  @ViewChild('viewport') viewportRef!: CdkVirtualScrollViewport;

  faBell = faBell;

  showNotificationDropdown = false;
  isLoadingMore = false;
  errorMessage: string | null = null;

  unreadCount = 0;

  notifications = signal<NotificationDTO[]>([]);

  private currentPage = 0;
  private pageSize = 5;
  private hasMore = true;
  private isFirstLoad = true;
  private pollSubscription?: Subscription;
  private scrollSubscription?: Subscription;

  constructor(private notificationService: NotificationService) { }

  // TrackBy function
  trackById = (index: number, notif: NotificationDTO): number => notif.id;

  ngOnInit() {
    this.loadUnreadCount();
    this.startPolling();
  }

  ngOnDestroy() {
    this.pollSubscription?.unsubscribe();
    this.scrollSubscription?.unsubscribe();
  }

  ngAfterViewInit() {
    // View đã initialized, scroll listener sẽ setup khi dropdown mở
  }

  private loadUnreadCount() {
    this.notificationService.getUnreadCount().subscribe({
      next: (res) => this.unreadCount = res.count,
      error: () => this.unreadCount = 0
    });
  }

  toggleDropdown() {
    this.showNotificationDropdown = !this.showNotificationDropdown;

    if (this.showNotificationDropdown) {
      this.resetAndLoadFirstPage();
      // setTimeout để chắc chắn view đã render xong trước khi setup listener
      setTimeout(() => {
        this.adjustPosition();
        this.setupScrollListener();
      }, 50);
    } else {
      this.cleanupScrollListener();
    }
  }

  private resetAndLoadFirstPage() {
    this.currentPage = 0;
    this.hasMore = true;
    this.notifications.set([]);
    this.loadMoreNotifications();
  }

  private loadMoreNotifications() {
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
        console.error(' Load notifications failed:', err);
        this.errorMessage = 'Không tải được thông báo';
        this.isLoadingMore = false;
      }
    });
  }

  // Trigger khi scroll
  private setupScrollListener() {
    if (!this.viewportRef) {
      console.error(' viewportRef chưa khởi tạo!');
      return;
    }

    this.scrollSubscription = fromEvent(this.viewportRef.elementRef.nativeElement, 'scroll')
      .pipe(debounceTime(200))
      .subscribe(() => {
        const viewport = this.viewportRef.elementRef.nativeElement;
        const scrollTop = viewport.scrollTop;
        const clientHeight = viewport.clientHeight;
        const scrollHeight = viewport.scrollHeight;
        // Nếu scroll gần cuối (còn < 100px), load thêm
        if (scrollHeight - (scrollTop + clientHeight) < 100) {
          if (!this.isLoadingMore && this.hasMore) {
            this.loadMoreNotifications();
          }
        }
      });
  }

  private cleanupScrollListener() {
    this.scrollSubscription?.unsubscribe();
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

  adjustPosition() {
    if (!this.dropdownRef || !this.triggerRef) return;

    const dropdown = this.dropdownRef.nativeElement as HTMLElement;
    const trigger = this.triggerRef.nativeElement as HTMLElement;

    const rect = trigger.getBoundingClientRect();
    const dropdownWidth = dropdown.offsetWidth;
    const dropdownHeight = dropdown.offsetHeight;
    const screenWidth = window.innerWidth;
    const screenHeight = window.innerHeight;

    // Reset style trước
    dropdown.style.left = '';
    dropdown.style.right = '';
    dropdown.style.top = '';
    dropdown.style.bottom = '';

    // Horizontal
    if (rect.right + dropdownWidth > screenWidth) {
      dropdown.style.right = '5%';
    } else {
      dropdown.style.left = '5%';
    }

    // Vertical
    if (rect.bottom + dropdownHeight > screenHeight) {
      dropdown.style.bottom = '100%';
      dropdown.style.top = 'auto';
    } else {
      dropdown.style.top = '100%';
      dropdown.style.bottom = 'auto';
    }
  }

  @HostListener('document:click', ['$event'])
  handleClickOutside(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest('.notification-dropdown') && !target.closest('.notification-trigger')) {
      this.showNotificationDropdown = false;
    }
  }

  private startPolling() {
    this.pollSubscription = interval(30000).pipe(
      switchMap(() => this.notificationService.getUnreadCount())
    ).subscribe({
      next: (res) => this.unreadCount = res.count
    });
  }
}