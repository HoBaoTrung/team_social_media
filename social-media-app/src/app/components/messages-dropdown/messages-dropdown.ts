import { Component, HostListener, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faCommentDots } from '@fortawesome/free-solid-svg-icons';
import { TooltipDirective } from '../../shared/tooltip';

@Component({
  selector: 'app-messages-dropdown',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, TooltipDirective],
  templateUrl: './messages-dropdown.html',
  styleUrls: ['./messages-dropdown.css']
})
export class MessagesDropdownComponent {
  @ViewChild('dropdown') dropdownRef!: ElementRef;
  @ViewChild('trigger') triggerRef!: ElementRef;

  faCommentDots = faCommentDots;

  unreadCount = 3;
  isLoading = false;

  showMessageDropdown = false;

  conversations = [
    { id: 1, name: 'User A', lastMessage: 'Hello bro' },
    { id: 2, name: 'User B', lastMessage: 'How are you?' }
  ];

  toggleDropdown() {
    this.showMessageDropdown = !this.showMessageDropdown;

    if (this.showMessageDropdown) {
      setTimeout(() => this.adjustPosition(), 0);
    }
  }

  adjustPosition() {
    const dropdown = this.dropdownRef.nativeElement as HTMLElement;
    const trigger = this.triggerRef.nativeElement as HTMLElement;

    const rect = trigger.getBoundingClientRect();
    const dropdownWidth = dropdown.offsetWidth;
    const dropdownHeight = dropdown.offsetHeight;

    const screenWidth = window.innerWidth;
    const screenHeight = window.innerHeight;

    // reset trước
    dropdown.style.left = '';
    dropdown.style.right = '';
    dropdown.style.top = '';
    dropdown.style.bottom = '';

    //  kiểm tra overflow bên phải
    if (rect.right + dropdownWidth > screenWidth) {
      dropdown.style.right = '0';
    } else {
      dropdown.style.left = '0';
    }

    //  kiểm tra overflow phía dưới
    if (rect.bottom + dropdownHeight > screenHeight) {
      dropdown.style.bottom = '120%';
    } else {
      dropdown.style.top = '120%';
    }
  }

  // click ra ngoài thì đóng dropdown 
  @HostListener('document:click', ['$event'])
  handleClickOutside(event: Event) {
    const target = event.target as HTMLElement;

    if (!target.closest('.message-dropdown')) {
      this.showMessageDropdown = false;
    }
  }
}