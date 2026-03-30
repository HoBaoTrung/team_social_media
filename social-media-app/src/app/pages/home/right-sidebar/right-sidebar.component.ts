import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-right-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './right-sidebar.component.html'
})
export class RightSidebarComponent {
  contacts = [
    { id: 1, name: "Lê Thị C", avatar: "https://i.pravatar.cc/150?img=3", online: true },
    { id: 2, name: "Phạm Văn D", avatar: "https://i.pravatar.cc/150?img=4", online: true },
    { id: 3, name: "Hoàng Thị E", avatar: "https://i.pravatar.cc/150?img=5", online: false },
    { id: 4, name: "Đặng Minh F", avatar: "https://i.pravatar.cc/150?img=6", online: true },
    { id: 5, name: "Vũ Ngọc G", avatar: "https://i.pravatar.cc/150?img=7", online: true },
  ];
}