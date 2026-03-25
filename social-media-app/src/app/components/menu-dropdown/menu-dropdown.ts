import { Component, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faBars, faHome, faUserFriends, faUsers,
  faHistory, faBookmark, faVideo, faStore
} from '@fortawesome/free-solid-svg-icons';
import { DevelopingAlert } from '../../services/developing.service';
import { TooltipDirective } from '../../shared/tooltip';
import { MenuContent } from '../menu-content/menu-content';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule, TooltipDirective, MenuContent],
  templateUrl: './menu-dropdown.html',
  styleUrls: ['./menu-dropdown.css']
})
export class MenuComponent {

  alertDeveloping = inject(DevelopingAlert);

  faBars = faBars;
  faHome = faHome;
  faUserFriends = faUserFriends;
  faUsers = faUsers;
  faHistory = faHistory;
  faBookmark = faBookmark;
  faVideo = faVideo;
  faStore = faStore;

  showMenuDropdown = false; 

  menuItems = [
    { icon: faHome, label: 'Home', link: '/news-feed', developing: false },
    { icon: faUserFriends, label: 'Bạn bè', link: '/friends', developing: false },
    { icon: faUsers, label: 'Nhóm', link: '#', developing: true },
    { icon: faHistory, label: 'Kỷ niệm', link: '#', developing: true },
    { icon: faBookmark, label: 'Đã lưu', link: '#', developing: true },
    { icon: faVideo, label: 'Video', link: '#', developing: true },
    { icon: faStore, label: 'Marketplace', link: '#', developing: true }
  ];

  toggleMenu() {
    this.showMenuDropdown = !this.showMenuDropdown;
  }

  closeMenu() {
    this.showMenuDropdown = false;
  }

  // click ra ngoài là đóng
  @HostListener('document:click', ['$event'])
  handleClickOutside(event: Event) {
    const target = event.target as HTMLElement;

    if (!target.closest('.menu-dropdown')) { 
      this.closeMenu();
    }
  }
}