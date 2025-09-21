package com.codegym.socialmedia.service.chat;

import com.codegym.socialmedia.dto.chat.ConversationDto;
import com.codegym.socialmedia.dto.chat.MessageDto;
import com.codegym.socialmedia.dto.chat.SendMessageRequest;
import com.codegym.socialmedia.dto.chat.UserSearchDto;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.conversation.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatService {
    Page<ConversationDto> getConversationsForUser(Long userId, Pageable pageable);
    List<MessageDto> getChatHistory(Long userId1, Long userId2);
    MessageDto sendMessage(Long senderId, SendMessageRequest request);
    Page<ConversationDto> getOnlineFriends(Long userId, Pageable pageable);
    Conversation createGroupConversation(User creator, List<User> participants, String groupName);
    ConversationDto findOrCreatePrivateConversation(Long currentUserId, Long targetUserId);
    List<MessageDto> getMessagesByConversation(Long conversationId, int page, int size);
    List<ConversationDto.ParticipantDto> getParticipants(Long conversationId);
    ConversationDto createGroupFromIds(Long creatorId, List<Long> participantIds, String groupName);
    String updateGroupAvatar(Long conversationId, MultipartFile file);
    Long getTotalUnread(Long userId);
    Long getUnreadCount(Long conversationId, Long userId);
    void markAsRead(Long conversationId, Long userId);
    List<UserSearchDto> searchUsers(String keyword, Long currentUserId);
    List<ConversationDto> getGroupsForUser(Long userId);
}
