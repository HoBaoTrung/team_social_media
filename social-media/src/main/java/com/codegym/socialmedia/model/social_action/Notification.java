package com.codegym.socialmedia.model.social_action;

import com.codegym.socialmedia.model.account.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;  // ai thực hiện hành động

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;  // ai nhận thông báo

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 50) // Thêm length
    private NotificationType notificationType;


    private Long referenceId;

    private boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum NotificationType {
        LIKE_POST,
        LIKE_COMMENT,
        COMMENT_POST,
        REPLY_COMMENT,
        FRIEND_REQUEST,
        MENTION_COMMENT
    }
}

