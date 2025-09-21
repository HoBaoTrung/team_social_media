package com.codegym.socialmedia.repository;

import com.codegym.socialmedia.model.conversation.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
        SELECT DISTINCT c FROM Conversation c 
        JOIN c.participants p 
        WHERE p.user.id = :userId AND p.isActive = true
        ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC
    """)
    Page<Conversation> findConversationsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT DISTINCT c FROM Conversation c 
        JOIN c.participants p 
        WHERE p.user.id = :userId AND p.isActive = true and c.conversationType = 'PRIVATE'
        ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC
    """)
    Page<Conversation> findPrivateConversationsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT c FROM Conversation c 
        WHERE c.conversationType = 'PRIVATE'
        AND c.id IN (
            SELECT cp1.conversation.id FROM ConversationParticipant cp1
            WHERE cp1.user.id = :user1Id AND cp1.isActive = true
        )
        AND c.id IN (
            SELECT cp2.conversation.id FROM ConversationParticipant cp2
            WHERE cp2.user.id = :user2Id AND cp2.isActive = true
        )
        AND (
            SELECT COUNT(cp) FROM ConversationParticipant cp 
            WHERE cp.conversation.id = c.id AND cp.isActive = true
        ) = 2
    """)
    Optional<Conversation> findPrivateConversationBetweenUsers(@Param("user1Id") Long user1Id,
                                                               @Param("user2Id") Long user2Id);

    @Query("""
        SELECT COUNT(cp) > 0 FROM ConversationParticipant cp
        WHERE cp.conversation.id = :conversationId 
          AND cp.user.id = :userId 
          AND cp.isActive = true
    """)
    boolean isUserInConversation(@Param("conversationId") Long conversationId,
                                 @Param("userId") Long userId);

    // NHÓM có user tham gia
    @Query("""
        SELECT DISTINCT c FROM Conversation c
        JOIN c.participants p
        WHERE c.conversationType = 'GROUP'
          AND p.user.id = :userId
          AND p.isActive = true
        ORDER BY COALESCE(c.lastMessageAt, c.updatedAt) DESC NULLS LAST, c.createdAt DESC
    """)
    List<Conversation> findGroupsByMemberId(@Param("userId") Long userId);
}
