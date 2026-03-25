
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';  
import { HeaderComponent } from '../../components/header/header';
import { NavbarContainerComponent } from '../../components/navbar-container/navbar-container';

@Component({
  selector: 'app-main-layout',  
  standalone: true,
  imports: [
    RouterModule,              
    HeaderComponent
  ],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.css'
})
export class MainLayoutComponent {
  
}