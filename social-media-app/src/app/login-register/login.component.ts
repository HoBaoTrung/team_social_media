import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { faFacebookF, faGoogle } from '@fortawesome/free-brands-svg-icons';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'login-register',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FontAwesomeModule
  ]
})
export class LoginComponent {
  faGoogle = faGoogle;
  faFacebook = faFacebookF;

  loginForm!: FormGroup;
  registerForm!: FormGroup;

  isRegisterMode = false;
  errorMessage = '';
  successMessage = '';

  maxDate = new Date(new Date().setFullYear(new Date().getFullYear() - 16))
    .toISOString()
    .split('T')[0];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.initializeForms();
  }

  private initializeForms() {
    // LOGIN FORM
    this.loginForm = this.fb.group({
      login_username: ['', [Validators.required]],
      login_password: ['', [Validators.required]]
    });

    // REGISTER FORM
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: [''],
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^(0[0-9]{9,10}|\+84[0-9]{9,10})$/)]],
      dateOfBirth: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator.bind(this) });
  }

  private passwordMatchValidator(form: FormGroup): { [key: string]: any } | null {
    const password = form.get('password')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;

    if (!password || !confirmPassword) return null;

    if (password !== confirmPassword) {
      return { mismatch: true };
    }
    return null;
  }

  login(): void {
    this.errorMessage = '';
    if (this.loginForm.invalid) {
      this.errorMessage = 'Vui lòng nhập tên đăng nhập và mật khẩu';
      return;
    }

    this.authService.login(
      this.loginForm.value.login_username,
      this.loginForm.value.login_password
    ).subscribe({
      next: () => {
        this.loginForm.reset();
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Tên đăng nhập hoặc mật khẩu không đúng';
      }
    });
  }

  register(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.registerForm.invalid) {
      this.errorMessage = 'Vui lòng điền đầy đủ và chính xác tất cả thông tin bắt buộc';
      return;
    }

    const registerData = { ...this.registerForm.value };
    delete registerData.confirmPassword;

    this.authService.register(registerData).subscribe({
      next: () => {
        this.successMessage = 'Đăng ký thành công! Chuyển hướng...';
        this.registerForm.reset();
        setTimeout(() => {
          this.switchToLogin();
        }, 2000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Đăng ký thất bại. Vui lòng thử lại';
      }
    });
  }

  switchToLogin(): void {
    this.registerForm.disable();
    this.loginForm.enable();
    this.isRegisterMode = false;
    this.errorMessage = '';
    this.successMessage = '';
    this.registerForm.reset();
  }

  switchToRegister(): void {
    this.loginForm.disable();
    this.registerForm.enable();
    this.isRegisterMode = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.loginForm.reset();
  }
}
