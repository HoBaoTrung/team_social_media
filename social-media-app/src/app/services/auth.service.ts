import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { catchError, map, tap, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { jwtDecode } from 'jwt-decode';

export interface LoginResponse {
  access_token: string;
  id: number;
  username: string;
  fullName: string;
  avatarUrl: string;
  roles?: string[];
}

export interface UserProfile {
  id: number;
  username: string;
  fullName: string;
  avatarUrl: string;
  roles?: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  private accessToken: string | null = null;

  private refreshing = false;            // trạng thái refresh
  private refreshQueue: Array<() => void> = [];

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    // Khôi phục user info từ localStorage
    const storedUser = localStorage.getItem('currentUser');
    if (storedUser) {
      try {
        this.currentUserSubject.next(JSON.parse(storedUser));
      } catch {
        localStorage.removeItem('currentUser');
      }
    }
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  isTokenExpired(token: string): boolean {
    try {
      const decoded: any = jwtDecode(token);
      return decoded.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }

  isLoggedIn(): boolean {
    const token = this.accessToken;
    return !!token && !this.isTokenExpired(token);
  }

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${environment.apiUrl}/auth/login`,
      { username, password },
      { withCredentials: true }
    ).pipe(
      tap(res => {
        this.accessToken = res.access_token;
        const userData = {
          id: res.id,
          username: res.username,
          fullName: res.fullName,
          avatarUrl: res.avatarUrl,
          roles: res.roles || []
        };
        localStorage.setItem('currentUser', JSON.stringify(userData));
        this.currentUserSubject.next(userData);
      }),
      catchError(err => throwError(() => err))
    );
  }

  logout(): void {
    this.accessToken = null;
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
    this.router.navigate(['/auth/login']);

    // Gọi API logout để xóa refresh token server side
    this.http.post(`${environment.apiUrl}/auth/logout`, {}, { withCredentials: true }).subscribe();
  }

  refreshAccessToken(): Observable<any> {
    if (this.refreshing) {
      // Nếu đang refresh, return Observable chờ queue
      return new Observable(observer => {
        this.refreshQueue.push(() => {
          observer.next({ access_token: this.accessToken! });
          observer.complete();
        });
      });
    }

    this.refreshing = true;

    return this.http.post<{ access_token: string }>(
      `${environment.apiUrl}/auth/refresh`,
      {},
      { withCredentials: true }
    ).pipe(
      tap(res => {
        this.accessToken = res.access_token;

        // thông báo các request chờ queue
        this.refreshQueue.forEach(cb => cb());
        this.refreshQueue = [];
      }),
      catchError(err => {
        // Nếu refresh fail → logout
        console.error('Refresh token error:', err);
        this.logout();
        return throwError(() => err);
      }),
      tap(() => this.refreshing = false)
    );
  }

  getCurrentUser(): any {
    return this.currentUserSubject.value;
  }

  // getProfile(): Observable<UserProfile> {
  //   return this.http.get<UserProfile>(`${environment.apiUrl}/auth/me`, { withCredentials: true });
  // }

  hasAnyRole(roles: string[]): boolean { const user = this.getCurrentUser(); if (!user || !user.roles) return false; return roles.some(role => user.roles.includes(role)); }

  register(registerData: any): Observable<any> { return this.http.post<any>(`${environment.apiUrl}/auth/register`, registerData) .pipe( catchError(err => { console.error('Register error:', err); return throwError(() => err); }) ); }
}