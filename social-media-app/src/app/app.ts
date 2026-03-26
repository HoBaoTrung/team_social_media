import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  protected readonly title = signal('social-media-app');

  private authService = inject(AuthService);

  ngOnInit(): void {
    // Khi app load → restore session từ refresh token (stored in HttpOnly cookie)
    this.authService.refreshAccessToken().subscribe({
      next: (res) => {
        console.log('Session restored successfully');
      },
      error: (err) => {
        console.log('No active session or refresh failed - user needs to login');
      }
    });
  }
}