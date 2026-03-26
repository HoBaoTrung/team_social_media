import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);

  // Bỏ qua auth requests (login, register, refresh, logout, me)
  if (
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/register') ||
    req.url.includes('/auth/refresh') ||
    req.url.includes('/auth/logout') ||
    req.url.includes('/auth/me')
  ) {
    return next(req);
  }

  const token = authService.getAccessToken();
  let authReq = req;

  if (token) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(authReq).pipe(
    catchError(err => {
      if (err.status !== 401) return throwError(() => err);

      // Token expired → refresh rồi retry (chỉ 1 lần)
      return authService.refreshAccessToken().pipe(
        switchMap(res => {
          const newToken = res.access_token;
          const retryReq = req.clone({
            setHeaders: { Authorization: `Bearer ${newToken}` }
          });
          return next(retryReq);
        }),
        catchError(refreshErr => {
          // Refresh fail → logout + throw error
          console.error('Token refresh failed:', refreshErr);
          return throwError(() => refreshErr);
        })
      );
    })
  );
};