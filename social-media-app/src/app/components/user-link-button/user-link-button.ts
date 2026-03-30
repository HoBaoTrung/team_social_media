// user-dropdown.component.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

import { AuthService } from '../../services/auth.service';

@Component({
    selector: 'app-user-link-button',
    standalone: true,
    imports: [CommonModule, RouterModule, FontAwesomeModule],
    templateUrl: './user-link-button.html',
    styles: `
            a:hover {
            color: #212529;
            background-color: #f8f9fa;
            }
        a{
                padding: 5px;
            }
        `

})
export class UserLinkButtonComponent {
    authService = inject(AuthService);
    user$ = this.authService.currentUser$;
    isAdmin = this.authService.hasAnyRole(['ADMIN']);

} 