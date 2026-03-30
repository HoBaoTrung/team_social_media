import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChatWindowComponent } from '../../../components/chat-window/chat-window';
// import { CreatePostComponent } from './create-post.component';
// import { PostComponent } from './post.component';
// CreatePostComponent, PostComponent
@Component({
  selector: 'app-feed',
  standalone: true,
  imports: [CommonModule, ChatWindowComponent],
  templateUrl: './feed.component.html'
})
export class FeedComponent {
  posts = [
    {
      id: 1,
      user: "Nguyễn Văn A",
      avatar: "https://i.pravatar.cc/150?img=1",
      time: "2 giờ trước",
      content: "Hôm nay thời tiết đẹp quá mọi người ơi! Ai rảnh thì đi cà phê không? ☕",
      image: "https://picsum.photos/id/1015/600/400",
      likes: 124,
      comments: 18,
      shares: 5
    },
    {
      id: 2,
      user: "Trần Thị B",
      avatar: "https://i.pravatar.cc/150?img=2",
      time: "5 giờ trước",
      content: "Mới mua được con MacBook mới, ai có kinh nghiệm dùng thì cho mình xin ít tips với ạ!",
      likes: 89,
      comments: 32,
      shares: 12
    }
  ];
}