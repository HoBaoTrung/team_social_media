// user-dropdown.component.ts
import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faCog, faSignOutAlt, faUserShield } from '@fortawesome/free-solid-svg-icons';
import { Observable } from 'rxjs';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-user-dropdown',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule],
  templateUrl: './user-dropdown.html',
  styleUrls: ['./user-dropdown.css']
})
export class UserDropdownComponent implements OnInit {
  authService = inject(AuthService);
  

  user$!: Observable<any>;
  isAdmin: boolean = false;

  faCog = faCog;
  faSignOutAlt = faSignOutAlt;
  faUserShield = faUserShield;

  ngOnInit(): void {
    this.user$ = this.authService.currentUser$;
    this.isAdmin = this.authService.hasAnyRole(['ADMIN']);
  }

} 