package com.codegym.socialmedia.repository.user;


import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.account.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    // Tìm theo token
    Optional<UserSession> findBySessionToken(String sessionToken);

    // Tìm session đang hoạt động của người dùng
    List<UserSession> findByUserAndIsActiveTrue(User user);

    // Tìm session hợp lệ (đang hoạt động và chưa hết hạn)
    Optional<UserSession> findBySessionTokenAndIsActiveTrue(String sessionToken);

    // Xoá session hết hạn
    void deleteByExpiresAtBefore(java.time.LocalDateTime time);

    void deleteByUser(User user);
}
