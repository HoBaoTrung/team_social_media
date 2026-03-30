

import { Component } from '@angular/core';
import { LeftMenuComponent } from './left-menu/left-menu.component';
import { FeedComponent } from './feed/feed.component';
import { RightSidebarComponent } from './right-sidebar/right-sidebar.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    LeftMenuComponent,
    FeedComponent,
    RightSidebarComponent
  ],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent { }