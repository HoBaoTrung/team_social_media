
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faBars, faCommentDots, faBell, faSignInAlt } from '@fortawesome/free-solid-svg-icons';

import { AuthService } from '../../services/auth.service'; 
import { MenuComponent } from '../menu-dropdown/menu-dropdown';
import { MessagesDropdownComponent } from '../messages-dropdown/messages-dropdown';
import { UserDropdownComponent } from '../user-dropdown/user-dropdown';
import { LoginIconComponent } from '../login-icon/login-icon';
import { NotificationsDropdown } from '../notifications-dropdown/notifications-dropdown';

@Component({
  selector: 'app-navbar-container',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FontAwesomeModule,
    MenuComponent,
    MessagesDropdownComponent,
    NotificationsDropdown,
    UserDropdownComponent,
    LoginIconComponent
  ],
  templateUrl: './navbar-container.html',
  styleUrls: ['./navbar-container.css']
})
export class NavbarContainerComponent {
  private authService = inject(AuthService);

  isAuthenticated = this.authService.isAuthenticated(); 

  faBars = faBars;
  faCommentDots = faCommentDots;
  faBell = faBell;
  faSignInAlt = faSignInAlt;
}