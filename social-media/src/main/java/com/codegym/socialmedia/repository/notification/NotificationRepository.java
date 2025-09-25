package com.codegym.socialmedia.repository.notification;

import com.codegym.socialmedia.model.social_action.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByReceiverId(Long receiverId, Pageable pageable);
    long countByReceiverIdAndIsReadFalse(Long receiverId);
}
