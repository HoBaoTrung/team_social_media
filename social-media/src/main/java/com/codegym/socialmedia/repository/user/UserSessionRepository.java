package com.codegym.socialmedia.repository.user;


import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.account.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByRefreshTokenAndIsActiveTrue(String refreshToken);
    Optional<UserSession> findByRefreshToken(String refreshToken);

    // Xoá session hết hạn
    void deleteByExpiresAtBefore(java.time.LocalDateTime time);

    void deleteByUser(User user);
}
