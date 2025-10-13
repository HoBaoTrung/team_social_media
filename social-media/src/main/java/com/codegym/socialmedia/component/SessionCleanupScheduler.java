package com.codegym.socialmedia.component;

import com.codegym.socialmedia.repository.user.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@EnableScheduling
public class SessionCleanupScheduler {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Scheduled(cron = "0 0 3 * * SUN") // mỗi chủ nhật 3 giờ sáng
    public void cleanupOldSessions() {
        userSessionRepository.deleteByExpiresAtBefore(LocalDateTime.now().minusWeeks(1));
    }

}
