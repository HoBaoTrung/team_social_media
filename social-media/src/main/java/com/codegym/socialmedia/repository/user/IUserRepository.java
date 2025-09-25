package com.codegym.socialmedia.repository.user;

import com.codegym.socialmedia.model.account.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    User findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByCreatedAtAfter(LocalDateTime date);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        SELECT u FROM User u
        WHERE (LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    List<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE u.id <> :currentUserId
          AND (LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    List<User> searchUsersExcludeCurrent(@Param("keyword") String keyword,
                                         @Param("currentUserId") Long currentUserId,
                                         Pageable pageable);

    @Query("""
        SELECT u FROM User u
        WHERE (LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND u.id <> :currentUserId
          AND NOT EXISTS (
            SELECT 1 FROM ConversationParticipant cp
            WHERE cp.conversation.id = :conversationId
              AND cp.user.id = u.id
              AND cp.isActive = true
          )
    """)
    List<User> searchUsersNotInConversation(@Param("keyword") String keyword,
                                            @Param("conversationId") Long conversationId,
                                            @Param("currentUserId") Long currentUserId,
                                            Pageable pageable);
    Page<User> findByIdNot(Long id, Pageable pageable);
    // Nếu bạn muốn tìm theo cả tên + họ
    @Query("SELECT u FROM User u " +
            "WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);
}