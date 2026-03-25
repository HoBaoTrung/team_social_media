import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

// Icons (Font Awesome)
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import {
  faHome,
  faUserFriends,
  faStore,
  faUsers,
  faBell,
  faCommentDots,
  faBars,
  faSignInAlt,
  faCog,
  faUserShield,
  faSignOutAlt,
} from '@fortawesome/free-solid-svg-icons';
import { NavbarButton } from '../navbar-button/navbar-button';
import { DevelopingAlert } from '../../services/developing.service';
import { NavbarContainerComponent } from '../navbar-container/navbar-container';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule, NavbarContainerComponent,
    FontAwesomeModule, NavbarButton
  ],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class HeaderComponent implements OnInit {

  // Icons
  faHome = faHome;
  faUserFriends = faUserFriends;
  faStore = faStore;
  faUsers = faUsers;
  faBell = faBell;
  faCommentDots = faCommentDots;
  faBars = faBars;
  faSignInAlt = faSignInAlt;
  faCog = faCog;
  faUserShield = faUserShield;
  faSignOutAlt = faSignOutAlt;

  // Trạng thái
  isAuthenticated = false;
  currentUser: any = null;           // { id, username, fullName, avatarUrl, roles }
  searchQuery = '';
  messageCount = 0;
  notificationCount = 0;
  messagesLoaded = false;
  // Dropdown states (vì Bootstrap dropdown đôi khi conflict với Angular)
  showMessageDropdown = false;
  showMenuDropdown = false;
  showNotificationDropdown = false;

  private authService = inject(AuthService);
  alertDeveloping = inject(DevelopingAlert);

  ngOnInit() {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.isAuthenticated = !!user;
      // Nếu có, load message/notification count ở đây
    });

    // Có thể load initial notifications / messages
  }

  toggleMessageDropdown() {
    this.showMessageDropdown = !this.showMessageDropdown;
    // Đóng các dropdown khác nếu cần
    this.showMenuDropdown = false;
    this.showNotificationDropdown = false;
  }

  toggleMenuDropdown() {
    this.showMenuDropdown = !this.showMenuDropdown;
    this.showMessageDropdown = false;
    this.showNotificationDropdown = false;
  }

  toggleNotificationDropdown() {
    this.showNotificationDropdown = !this.showNotificationDropdown;
    this.showMessageDropdown = false;
    this.showMenuDropdown = false;
  }

  logout() {
    this.authService.logout();
  }


}