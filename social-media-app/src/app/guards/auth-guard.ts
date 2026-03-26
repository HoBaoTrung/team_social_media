import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(
    private authService: AuthService,
    private router: Router
  ) { }

  canActivate(): Observable<boolean> {
    // Nếu đã có token và chưa hết hạn → allow
    if (this.authService.isLoggedIn()) {
      return of(true);
    }

    // Nếu không có token hoặc hết hạn → thử refresh từ server
    return this.authService.refreshAccessToken().pipe(
      map(() => {
        // Refresh thành công → allow
        return true;
      }),
      catchError((err) => {
        // Refresh fail → redirect tới login
        this.router.navigate(['/auth/login']);
        return of(false);
      })
    );
  }
}