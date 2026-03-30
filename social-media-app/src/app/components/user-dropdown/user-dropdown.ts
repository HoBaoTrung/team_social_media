// user-dropdown.component.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faCog, faSignOutAlt, faUserShield } from '@fortawesome/free-solid-svg-icons';

import { AuthService } from '../../services/auth.service';
import { UserLinkButtonComponent } from '../user-link-button/user-link-button';

@Component({
  selector: 'app-user-dropdown',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule, UserLinkButtonComponent],
  templateUrl: './user-dropdown.html',
  styleUrls: ['./user-dropdown.css']
})
export class UserDropdownComponent {
  authService = inject(AuthService);
  user$ = this.authService.currentUser$;
  isAdmin = this.authService.hasAnyRole(['ADMIN']);

  faCog = faCog;
  faSignOutAlt = faSignOutAlt;
  faUserShield = faUserShield;


} 