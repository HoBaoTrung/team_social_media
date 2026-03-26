import { ElementRef, ViewChild, HostListener, signal, OnDestroy, Directive } from '@angular/core';
import { CdkVirtualScrollViewport } from '@angular/cdk/scrolling';
import { Subscription, interval, fromEvent, Observable } from 'rxjs';
import { switchMap, debounceTime } from 'rxjs/operators';

/**
 * Base class cho tất cả dropdown components (notification, message, etc)
 * Cung cấp logic chung cho: toggle, positioning, scroll loading, polling
 */
@Directive()
export abstract class DropdownBaseComponent implements OnDestroy {
  @ViewChild('dropdown') dropdownRef!: ElementRef;
  @ViewChild('trigger') triggerRef!: ElementRef;
  @ViewChild('viewport') viewportRef?: CdkVirtualScrollViewport;

  showDropdown = false;
  isLoadingMore = false;
  errorMessage: string | null = null;
  unreadCount = 0;

  protected currentPage = 0;
  protected pageSize = 5;
  protected hasMore = true;
  protected isFirstLoad = true;
  private pollSubscription?: Subscription;
  private scrollSubscription?: Subscription;
  protected pollingInterval = 3000000; // 3000 seconds
  protected scrollThreshold = 100; // pixels from bottom

  ngOnDestroy() {
    this.pollSubscription?.unsubscribe();
    this.scrollSubscription?.unsubscribe();
  }

  
  toggleDropdown() {
    this.showDropdown = !this.showDropdown;

    if (this.showDropdown) {
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
    this.isFirstLoad = true;
    this.errorMessage = null;
    this.loadInitialData();
  }

  
  private setupScrollListener() {
    if (!this.viewportRef) {
      console.error('viewportRef chưa khởi tạo!');
      return;
    }

    this.scrollSubscription = fromEvent(this.viewportRef.elementRef.nativeElement, 'scroll')
      .pipe(debounceTime(200))
      .subscribe(() => {
        const viewport = this.viewportRef!.elementRef.nativeElement;
        const scrollTop = viewport.scrollTop;
        const clientHeight = viewport.clientHeight;
        const scrollHeight = viewport.scrollHeight;

        // Nếu scroll gần cuối (còn < scrollThreshold px), load thêm
        if (scrollHeight - (scrollTop + clientHeight) < this.scrollThreshold) {
          if (!this.isLoadingMore && this.hasMore) {
            this.loadMoreData();
          }
        }
      });
  }

  
  private cleanupScrollListener() {
    this.scrollSubscription?.unsubscribe();
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
    const dropdownSelector = this.getDropdownSelector();
    const triggerSelector = this.getTriggerSelector();

    if (!target.closest(dropdownSelector) && !target.closest(triggerSelector)) {
      this.showDropdown = false;
    }
  }

  
  protected startPolling() {
    this.pollSubscription = interval(this.pollingInterval).pipe(
      switchMap(() => this.fetchUnreadCount())
    ).subscribe({
      next: (res: { count: number }) => this.unreadCount = res.count,
      error: (err) => console.error('Polling error:', err)
    });
  }

  
  abstract loadInitialData(): void;
  abstract loadMoreData(): void;
  abstract fetchUnreadCount(): Observable<{ count: number }>;
  abstract getDropdownSelector(): string;
  abstract getTriggerSelector(): string;
}
