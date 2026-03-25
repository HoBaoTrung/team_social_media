import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, throwError, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { jwtDecode } from 'jwt-decode';

export interface LoginResponse {
  token: string;
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

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    // Restore user from localStorage when page reload
    try {
      const storedUser = localStorage.getItem('currentUser');
      if (storedUser) {
        this.currentUserSubject.next(JSON.parse(storedUser));
      }
    } catch (e) {
      console.error('Failed to parse stored user data:', e);
      localStorage.removeItem('currentUser');
    }
  }

  hasAnyRole(roles: string[]): boolean {
    const user = this.getCurrentUser();
    if (!user || !user.roles) return false;

    return roles.some(role => user.roles.includes(role));
  }

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, { username, password })
      .pipe(
        tap(response => {
          localStorage.setItem('jwt_token', response.token);

          const userData = {
            username: response.username,
            roles: response.roles || [],
            fullName: response.fullName,
            avatarUrl: response.avatarUrl
          };

          localStorage.setItem('currentUser', JSON.stringify(userData));
          this.currentUserSubject.next(userData);
        }),
        catchError(err => {
          console.error('Login error:', err);
          return throwError(() => err);
        })
      );
  }

  register(registerData: any): Observable<any> {
    // Giả sử backend có endpoint /auth/register
    return this.http.post<any>(`${environment.apiUrl}/auth/register`, registerData)
      .pipe(
        catchError(err => {
          console.error('Register error:', err);
          return throwError(() => err);
        })
      );
  }

  logout(): void {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
    this.router.navigate(['/auth/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('jwt_token');
  }

  isTokenExpired(token: string): boolean {
    try {
      const decoded: any = jwtDecode(token);
      return decoded.exp * 1000 < Date.now();
    } catch (e) {
      return true;
    }
  }

  public isAuthenticated(): Observable<boolean> {
    // 1. Check token local trước
    if (!this.isLoggedIn()) {
      return of(false);
    }

    // 2. Check backend
    return this.getProfile().pipe(
      map(() => true),
      catchError(() => {
        return of(false);
      })
    )
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) return false;

    return !this.isTokenExpired(token);
  }

  getProfile(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/auth/me`);
  }

  getCurrentUser(): any {
    return this.currentUserSubject.value;
  }
}