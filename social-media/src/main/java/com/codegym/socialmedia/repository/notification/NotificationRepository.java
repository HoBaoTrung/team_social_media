package com.codegym.socialmedia.repository.notification;

import com.codegym.socialmedia.model.social_action.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByReceiverId(Long receiverId, Pageable pageable);
    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.receiver.id = :receiverId and n.isRead = false")
    int markAllRead(Long receiverId);
}
