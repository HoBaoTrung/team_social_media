package com.codegym.socialmedia.repository.notification;

import com.codegym.socialmedia.model.account.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    NotificationSettings findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}