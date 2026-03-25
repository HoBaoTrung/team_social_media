// login-icon.component.ts
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faSignInAlt } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-login-icon',
  standalone: true,
  imports: [RouterModule, FontAwesomeModule],
  templateUrl: './login-icon.html',
  styleUrls: ['./login-icon.css']
})
export class LoginIconComponent {
  faSignInAlt = faSignInAlt;
}