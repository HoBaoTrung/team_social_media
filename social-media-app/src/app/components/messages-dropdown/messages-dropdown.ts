import { Component, OnInit, OnDestroy, ViewChild, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faCommentDots } from '@fortawesome/free-solid-svg-icons';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { RouterModule } from '@angular/router';

import { MessageService, ConversationDto } from '../../services/message.service';
import { DropdownBaseComponent } from '../shared/dropdown-base.component';
import { Observable } from 'rxjs';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-messages-dropdown',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, ScrollingModule, RouterModule],
  templateUrl: './messages-dropdown.html',
  styleUrls: ['./messages-dropdown.css', '../shared/dropdown-shared.css']
})
export class MessagesDropdownComponent extends DropdownBaseComponent implements OnInit {
  private auth = inject(AuthService);

  currentUser = this.auth.getCurrentUser();
  faCommentDots = faCommentDots;
  conversations = signal<ConversationDto[]>([]);

  constructor(private messageService: MessageService) {
    super();
  }

  ngOnInit() {
    this.loadUnreadCount();
    
    this.startPolling();
  }

  // TrackBy function
  trackById = (index: number, conv: ConversationDto): string => conv.id;


  private loadUnreadCount() {
    this.messageService.getUnreadCount(this.currentUser.id).subscribe({
      next: (res) =>this.unreadCount = +res,
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

    this.messageService.getConversations(this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        const currentList = this.conversations();

        if (this.isFirstLoad) {
          this.conversations.set(page.content);
          this.isFirstLoad = false;
        } else {
          this.conversations.set([...currentList, ...page.content]);
        }

        this.hasMore = !page.last;
        this.currentPage++;
        this.isLoadingMore = false;
      },
      error: (err) => {
        console.error('Load conversations failed:', err);
        this.errorMessage = 'Không tải được tin nhắn';
        this.isLoadingMore = false;
      }
    });
  }

 
  override fetchUnreadCount(): Observable<{ count: number }> {
    return this.messageService.getUnreadCount(this.currentUser.id);
  }


  override getDropdownSelector(): string {
    return '.message-dropdown';
  }

  
  override getTriggerSelector(): string {
    return '.message-trigger';
  }

  markAsRead(conversation: ConversationDto) {
  // Kiểm tra an toàn
  if (!conversation?.id) {
    console.warn('markAsRead: conversation.id is missing');
    return;
  }

  if (!conversation.hasUnread || conversation.unreadCount <= 0) {
    return; // Không có tin nhắn chưa đọc thì không cần gọi API
  }

  this.messageService.markAsRead(conversation.id, this.currentUser.id).subscribe({
    next: () => {
      console.log(`✅ Marked conversation ${conversation.id} as read`);

      // Cập nhật lại danh sách conversations trong signal
      this.conversations.update(list =>
        list.map(c => {
          if (c.id === conversation.id) {
            return {
              ...c,
              hasUnread: false,
              unreadCount: 0,
              lastMessage: c.lastMessage   // giữ nguyên (string)
            };
          }
          return c;
        })
      );

      // Giảm tổng unreadCount ở dropdown
      this.unreadCount = Math.max(0, this.unreadCount - conversation.unreadCount);
    },
    error: (err) => {
      console.error('Failed to mark as read:', err);
     
    }
  });
}
}