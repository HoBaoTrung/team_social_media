package com.codegym.socialmedia.repository.conversation;

import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.conversation.Conversation;
import com.codegym.socialmedia.model.conversation.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    // Lấy participants của một cuộc trò chuyện
    @Query("""
        SELECT cp FROM ConversationParticipant cp
        WHERE cp.conversation.id = :conversationId AND cp.isActive = true
        ORDER BY cp.joinedAt
    """)
    List<ConversationParticipant> findByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT c FROM Conversation c JOIN FETCH c.participants WHERE c.id = :id")
    Optional<Conversation> findByIdWithParticipants(@Param("id") Long id);
    List<ConversationParticipant> findByUserId(Long userId);
    Optional<ConversationParticipant> findByConversationIdAndUserId(Long conversationId, Long userId);

    @Query("SELECT cp.user FROM ConversationParticipant cp WHERE cp.conversation.id = :conversationId")
    List<User> findUsersByConversationId(@Param("conversationId") Long conversationId);
}