package com.codegym.socialmedia.model.account;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
@Entity
@Table(name = "notifications_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettings {

    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    public NotificationSettings(User user) {
        this.user = user;
        this.id = user.getId();
    }

    private boolean friendRequests = true;
    private boolean messages = true;
    private boolean statusLikes = true;

    private boolean statusComments = true;
    private boolean statusShares = true;

    private boolean friendStatusUpdates = true;
    private boolean systemNotifications = false;

    private boolean emailNotifications = false;
    private boolean pushNotifications = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
