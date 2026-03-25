import { Component, inject } from '@angular/core';
import {
  faBars, faHome, faUserFriends, faUsers,
  faHistory, faBookmark, faVideo, faStore
} from '@fortawesome/free-solid-svg-icons';
import { DevelopingAlert } from '../../services/developing.service';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
@Component({
  standalone: true,
  selector: 'app-menu-content',
  imports: [CommonModule, RouterModule, FontAwesomeModule],
  templateUrl: './menu-content.html',
  styleUrl: './menu-content.css',
})
export class MenuContent {
  alertDeveloping = inject(DevelopingAlert);

  faBars = faBars;
  faHome = faHome;
  faUserFriends = faUserFriends;
  faUsers = faUsers;
  faHistory = faHistory;
  faBookmark = faBookmark;
  faVideo = faVideo;
  faStore = faStore;

  menuItems = [
    { icon: faHome, label: 'Home', link: '/news-feed', developing: false },
    { icon: faUserFriends, label: 'Bạn bè', link: '/friends', developing: false },
    { icon: faUsers, label: 'Nhóm', link: '#', developing: true },
    { icon: faHistory, label: 'Kỷ niệm', link: '#', developing: true },
    { icon: faBookmark, label: 'Đã lưu', link: '#', developing: true },
    { icon: faVideo, label: 'Video', link: '#', developing: true },
    { icon: faStore, label: 'Marketplace', link: '#', developing: true }
  ];
}
