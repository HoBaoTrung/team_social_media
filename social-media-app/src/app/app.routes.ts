import { Routes } from '@angular/router';
import { LoginComponent } from './login-register/login.component';
import { AuthLayout } from './layouts/auth-layout/auth-layout';
import { MainLayoutComponent } from './layouts/main-layout/main-layout';
import { HomeComponent } from './pages/home/home';
import { AuthGuard } from './guards/auth-guard';


export const routes: Routes = [
  {
    path: 'auth',
    component: AuthLayout,
    children: [
      { path: 'login', component: LoginComponent },
      { path: '', redirectTo: 'login', pathMatch: 'full' },
    ]
  },

{
    path: '',
    component: MainLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      {
        path: 'home',
        component: HomeComponent
      }
    ]
  },
  {
    path: '**',
    redirectTo: 'home'
  }
];