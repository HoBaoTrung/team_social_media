
import { Component, Input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { IconDefinition } from '@fortawesome/free-solid-svg-icons';
import { TooltipDirective } from '../../shared/tooltip';

@Component({
  selector: 'app-navbar-button',
  standalone: true,
  imports: [FontAwesomeModule, RouterModule, TooltipDirective],
  templateUrl: './navbar-button.html',
  styleUrls: ['./navbar-button.css'],  
})
export class NavbarButton  {
  @Input() routerLink: string = '/';           
  @Input() title: string = 'Trang chủ';       
  @Input() icon!: IconDefinition;           
  @Input() activeClass: string = 'active';
}