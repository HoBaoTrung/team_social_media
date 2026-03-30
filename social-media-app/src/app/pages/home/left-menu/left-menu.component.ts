import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserLinkButtonComponent } from '../../../components/user-link-button/user-link-button';
import { MenuContent } from '../../../components/menu-content/menu-content';

@Component({
  selector: 'app-left-menu',
  standalone: true,
  imports: [CommonModule, UserLinkButtonComponent, MenuContent

  ],
  templateUrl: './left-menu.component.html',
  styleUrls: ['./left-menu.component.css']
})
export class LeftMenuComponent {
  menuItems = [
    { icon: 'fas fa-home text-3xl', label: 'Trang chủ', active: true },
    { icon: 'fas fa-users text-3xl', label: 'Bạn bè' },
    { icon: 'fas fa-user text-3xl', label: 'Trang cá nhân' },
    { icon: 'fas fa-clock text-3xl', label: 'Kỷ niệm' },
    { icon: 'fas fa-bookmark text-3xl', label: 'Đã lưu' },
    { icon: 'fas fa-flag text-3xl', label: 'Nhóm' },
    { icon: 'fas fa-users text-3xl', label: 'Cộng đồng' },
    { icon: 'fas fa-calendar text-3xl', label: 'Sự kiện' },
    { icon: 'fas fa-video text-3xl', label: 'Video' },
    { icon: 'fas fa-store text-3xl', label: 'Marketplace' },
  ];
}