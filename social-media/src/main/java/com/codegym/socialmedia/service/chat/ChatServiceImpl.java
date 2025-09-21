package com.codegym.socialmedia.service.chat;

import com.codegym.socialmedia.component.CloudinaryService;
import com.codegym.socialmedia.dto.chat.*;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.conversation.*;
import com.codegym.socialmedia.model.social_action.Notification;
import com.codegym.socialmedia.repository.ConversationParticipantRepository;
import com.codegym.socialmedia.repository.ConversationRepository;
import com.codegym.socialmedia.repository.IUserRepository;
import com.codegym.socialmedia.repository.MessageRepository;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import com.codegym.socialmedia.service.user.UserActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.HashSet;
import com.codegym.socialmedia.service.notification.NotificationService;
import com.codegym.socialmedia.model.social_action.Notification;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    @Autowired private ConversationRepository conversationRepository;
    @Autowired private ConversationParticipantRepository participantRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private IUserRepository userRepository;
    @Autowired private NotificationService notificationService;

    @Autowired private UserActivityService userActivityService;
    @Override
    public ConversationDto findOrCreatePrivateConversation(Long currentUserId, Long targetUserId) {
        Optional<Conversation> existing = conversationRepository
                .findPrivateConversationBetweenUsers(currentUserId, targetUserId);
        Conversation conv = existing.orElseGet(() -> createNewPrivateConversation(currentUserId, targetUserId));
        return mapToConversationDto(conv, currentUserId);
    }

    @Override
    public Page<ConversationDto> getConversationsForUser(Long userId, Pageable pageable) {
        Page<Conversation> conversationPage = conversationRepository.findConversationsByUserId(userId, pageable);

        Page<ConversationDto> dtoPage = conversationPage.map(conversation -> mapToConversationDto(conversation, userId));

        return dtoPage;
    }

    @Override
    public Page<ConversationDto> getOnlineFriends(Long userId, Pageable pageable) {
        User me = userRepository.findById(userId).orElse(null);
        if (me == null) {
            return Page.empty(pageable);
        }

        Page<Conversation> conversationPage = conversationRepository.findPrivateConversationsByUserId(userId, pageable);
        return conversationPage.map(conversation -> mapToConversationDto(conversation, userId));
    }

    @Override
    public MessageDto sendMessage(Long senderId, SendMessageRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Conversation conversation;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
            if (!isUserInConversation(conversation.getId(), senderId)) {
                throw new RuntimeException("User not in conversation");
            }
        } else if (request.getReceiverId() != null) {
            Optional<Conversation> existing = conversationRepository
                    .findPrivateConversationBetweenUsers(senderId, request.getReceiverId());
            conversation = existing.orElseGet(() -> createNewPrivateConversation(senderId, request.getReceiverId()));
        } else {
            throw new RuntimeException("Either conversationId or receiverId is required");
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setSentAt(LocalDateTime.now());
        message.setIsDeleted(false);
        message.setIsRecalled(false);

        // Handle attachments (existing code)
        if (!request.getAttachments().isEmpty()) {
            for (AttachmentDto a : request.getAttachments()) {
                MessageAttachment attachment = new MessageAttachment();
                attachment.setAttachmentUrl(a.getAttachmentUrl());
                attachment.setFileName(a.getFileName());
                attachment.setFileSize(a.getFileSize());
                attachment.setMessageType(Message.MessageType.valueOf(a.getType()));
                attachment.setMessage(message);
                message.getAttachments().add(attachment);
            }
        }

        // Set messageType logically
        if (request.getContent() != null && request.getContent().contains("call")) {
            message.setMessageType(Message.MessageType.CALL);
        } else if (message.getAttachments().isEmpty()) {
            message.setMessageType(Message.MessageType.TEXT);
        } else {
            message.setMessageType(message.getAttachments().get(0).getMessageType());
        }

        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Mark as read for sender
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversation.getId(), senderId).orElse(null);
        if (participant != null) {
            participant.setLastReadMessageId(saved.getMessageId());
            participantRepository.save(participant);
        }

        // **THÊM MỚI: Xử lý mention trong GROUP chat**
        if (conversation.getConversationType() == Conversation.ConversationType.GROUP &&
                request.getContent() != null && request.getContent().contains("@")) {
            handleMentionsInGroupMessage(saved, sender, conversation);
        }

        MessageDto dto = mapToMessageDto(saved);
        dto.setConversationId(conversation.getId());
        dto.setConversationType(conversation.getConversationType().name().toLowerCase());
        return dto;
    }

    private void handleMentionsInGroupMessage(Message message, User sender, Conversation conversation) {
        String content = message.getContent();
        if (content == null) return;

        Pattern mentionPattern = Pattern.compile("@(\\w+)");
        Matcher matcher = mentionPattern.matcher(content);

        Set<String> mentionedUsernames = new HashSet<>();
        while (matcher.find()) {
            mentionedUsernames.add(matcher.group(1));
        }

        if (mentionedUsernames.isEmpty()) return;

        List<ConversationParticipant> participants = participantRepository
                .findByConversationId(conversation.getId());

        for (ConversationParticipant participant : participants) {
            User user = participant.getUser();

            if (user.getId().equals(sender.getId())) continue;

            if (mentionedUsernames.contains(user.getUsername())) {
                // Gửi notification với conversationId để auto-open chat
                notificationService.notify(
                        sender.getId(),
                        user.getId(),
                        Notification.NotificationType.MENTION_COMMENT,
                        Notification.ReferenceType.POST, // Sử dụng POST type
                        conversation.getId() // Gửi conversationId thay vì message.getId()
                );
            }
        }
    }
    @Override
    public List<MessageDto> getChatHistory(Long userId1, Long userId2) {
        return conversationRepository.findPrivateConversationBetweenUsers(userId1, userId2)
                .map(c -> messageRepository.findMessagesByConversationId(c.getId(), Pageable.unpaged())
                        .getContent().stream().map(this::mapToMessageDto).collect(Collectors.toList()))
                .orElse(List.of());
    }


    @Override
    public Conversation createGroupConversation(User creator, List<User> participants, String groupName) {
        Conversation conversation = new Conversation();
        conversation.setConversationName(groupName);
        conversation.setConversationType(Conversation.ConversationType.GROUP);
        conversation.setCreatedBy(creator);
        conversation.setIsActive(true);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        Conversation saved = conversationRepository.save(conversation);

        ConversationParticipant admin = new ConversationParticipant();
        ConversationParticipantId id = new ConversationParticipantId();
        id.setConversationId(saved.getId());
        id.setUserId(creator.getId());
        admin.setId(id);
        admin.setConversation(saved);
        admin.setUser(creator);
        admin.setRole(ConversationParticipant.Role.ADMIN);
        admin.setJoinedAt(LocalDateTime.now());
        admin.setIsActive(true);
        participantRepository.save(admin);

        for (User p : participants) {
            if (Objects.equals(p.getId(), creator.getId())) continue;
            ConversationParticipant member = new ConversationParticipant();
            id = new ConversationParticipantId();
            id.setConversationId(saved.getId());
            id.setUserId(p.getId());
            member.setId(id);
            member.setConversation(saved);
            member.setUser(p);
            member.setRole(ConversationParticipant.Role.MEMBER);
            member.setJoinedAt(LocalDateTime.now());
            member.setIsActive(true);
            participantRepository.save(member);
        }
        return saved;
    }

    @Override
    public List<MessageDto> getMessagesByConversation(Long conversationId, int page, int size) {
        Page<Message> pageData = messageRepository.findMessagesByConversationId(conversationId, PageRequest.of(page, size));
        return pageData.getContent().stream().map(this::mapToMessageDto).collect(Collectors.toList());
    }

    @Override
    public List<ConversationDto.ParticipantDto> getParticipants(Long conversationId) {
        return participantRepository.findByConversationId(conversationId)
                .stream().map(this::mapParticipantDto).collect(Collectors.toList());
    }

    @Override
    public ConversationDto createGroupFromIds(Long creatorId, List<Long> participantIds, String groupName) {
        User creator = userRepository.findById(creatorId).orElseThrow(() -> new RuntimeException("Creator not found"));
        List<User> participants = userRepository.findAllById(participantIds);
        Conversation conv = createGroupConversation(creator, participants, groupName);
        return mapToConversationDto(conv, creatorId);
    }

    @Autowired
    private CloudinaryService cloudinaryService;

    @Override
    public String updateGroupAvatar(Long conversationId, MultipartFile file) {
        try {
            Conversation conv = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
            if (conv.getConversationType() != Conversation.ConversationType.GROUP)
                throw new RuntimeException("Only group conversation can update avatar");

            String url="https://res.cloudinary.com/dryyvmkwo/image/upload/v1748588721/samples/cloudinary-group.jpg";
            if (file != null && !file.isEmpty()) {
                url=cloudinaryService.upload(file);
            }

            conv.setGroupAvatar(url);

            conversationRepository.save(conv);
            return url;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update group avatar: " + e.getMessage(), e);
        }
    }

    @Override
    public Long getTotalUnread(Long userId) {
        return messageRepository.countTotalUnread(userId);
    }

    @Override
    public Long getUnreadCount(Long conversationId, Long userId) {
        ConversationParticipant cp = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Not participant"));

        Long lastRead = cp.getLastReadMessageId() == null ? 0 : cp.getLastReadMessageId();
        return messageRepository.countUnreadMessages(conversationId, lastRead);
    }

    @Override
    @Transactional
    public void markAsRead(Long conversationId, Long userId) {
        ConversationParticipant cp = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("Not participant"));

        Long latestMsgId = messageRepository.findLatestMessageId(conversationId);
        if (latestMsgId != null) {
            cp.setLastReadMessageId(latestMsgId);
            participantRepository.save(cp);
        }
    }

    // ====== BỔ SUNG ======
    @Override
    public List<UserSearchDto> searchUsers(String keyword, Long currentUserId) {
        Pageable limit = PageRequest.of(0, 10);
        return userRepository.searchUsersExcludeCurrent(keyword, currentUserId, limit)
                .stream().map(u -> {
                    UserSearchDto dto = new UserSearchDto();
                    dto.setId(u.getId());
                    dto.setUsername(u.getUsername());
                    dto.setFullName(safeFullName(u));
                    dto.setAvatarUrl(u.getProfilePicture());
                    dto.setFriend(true); // TODO: có thể check thật bằng FriendshipService
                    return dto;
                }).collect(Collectors.toList());
    }

    @Override
    public List<ConversationDto> getGroupsForUser(Long userId) {
        return conversationRepository.findGroupsByMemberId(userId)
                .stream()
                .map(c -> {
                    ConversationDto dto = mapToConversationDto(c, userId);

                    return dto;
                })
                .sorted((a, b) -> {
                    // Sắp xếp theo tin nhắn cuối hoặc thời gian tạo
                    LocalDateTime timeA = a.getLastMessageTime() != null ?
                            a.getLastMessageTime() : LocalDateTime.MIN;
                    LocalDateTime timeB = b.getLastMessageTime() != null ?
                            b.getLastMessageTime() : LocalDateTime.MIN;
                    return timeB.compareTo(timeA);
                })
                .collect(Collectors.toList());
    }


    // ===================== Helpers =====================

    private boolean isUserInConversation(Long conversationId, Long userId) {
        return participantRepository.findByConversationId(conversationId).stream()
                .anyMatch(p -> Objects.equals(p.getUser().getId(), userId) && Boolean.TRUE.equals(p.getIsActive()));
    }

    private Conversation createNewPrivateConversation(Long user1Id, Long user2Id) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new RuntimeException("User1 not found"));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new RuntimeException("User2 not found"));

        Conversation conv = new Conversation();
        conv.setConversationType(Conversation.ConversationType.PRIVATE);
        conv.setCreatedBy(user1);
        conv.setIsActive(true);
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        Conversation saved = conversationRepository.save(conv);

        // Participant 1
        ConversationParticipant p1 = new ConversationParticipant();
        p1.setId(new ConversationParticipantId(saved.getId(), user1.getId()));
        p1.setConversation(saved);
        p1.setUser(user1);
        p1.setRole(ConversationParticipant.Role.MEMBER);
        p1.setJoinedAt(LocalDateTime.now());
        p1.setIsActive(true);
        participantRepository.save(p1);

        // Participant 2
        ConversationParticipant p2 = new ConversationParticipant();
        p2.setId(new ConversationParticipantId(saved.getId(), user2.getId()));
        p2.setConversation(saved);
        p2.setUser(user2);
        p2.setRole(ConversationParticipant.Role.MEMBER);
        p2.setJoinedAt(LocalDateTime.now());
        p2.setIsActive(true);
        participantRepository.save(p2);

        return saved;
    }

    private ConversationDto.ParticipantDto mapParticipantDto(ConversationParticipant p) {
        User u = p.getUser();
        ConversationDto.ParticipantDto dto = new ConversationDto.ParticipantDto();
        dto.setId(u.getId());
        dto.setUsername(u.getUsername());
        dto.setFullName(safeFullName(u));
        dto.setAvatar(u.getProfilePicture());
        dto.setRole(p.getRole().name());
        // Set trạng thái online thực tế
        dto.setOnline(userActivityService.isOnline(u.getId()));
        return dto;
    }

    private MessageDto mapToMessageDto(Message m) {
        MessageDto dto = new MessageDto();
        dto.setId((long) m.getMessageId());
        dto.setConversationId(m.getConversation().getId());
        dto.setConversationType(m.getConversation().getConversationType().name().toLowerCase());
        dto.setSenderId(m.getSender().getId());
        dto.setSenderName(safeFullName(m.getSender()));
        dto.setSenderAvatar(m.getSender().getProfilePicture());
        dto.setContent(m.getContent());
        dto.setTimestamp(m.getSentAt());
        dto.setRead(m.getReadAt() != null);
        List<MessageAttachment> attachments = m.getAttachments();
        // Map attachments
        dto.setAttachments(m.getAttachments().stream().map(a -> {
            AttachmentDto ad = new AttachmentDto();
            ad.setAttachmentUrl(a.getAttachmentUrl());
            ad.setFileName(a.getFileName());
            ad.setFileSize(a.getFileSize());
            ad.setType(a.getMessageType().name());
            return ad;
        }).collect(Collectors.toList()));

        // Set primary type for DTO (for JS rendering): CALL if callStatus, else TEXT if no attachments, else first attachment type
        if (m.getCallStatus() != null) {
            dto.setType("CALL");
        } else if (m.getAttachments().isEmpty()) {
            dto.setType("TEXT");
        } else {
            dto.setType(m.getAttachments().get(0).getMessageType().name());
        }

        return dto;
    }

    private ConversationDto mapToConversationDto(Conversation c, Long currentUserId) {
        ConversationDto dto = new ConversationDto();
        dto.setId(c.getId().toString());
        dto.setType(c.getConversationType().name().toLowerCase());

        if (c.getConversationType() == Conversation.ConversationType.PRIVATE) {
            List<ConversationParticipant> ps = participantRepository.findByConversationId(c.getId());
            User other = ps.stream().map(ConversationParticipant::getUser)
                    .filter(u -> !u.getId().equals(currentUserId)).findFirst().orElse(null);
            if (other != null) {
                dto.setName(safeFullName(other));
                dto.setAvatar(other.getProfilePicture());
                // Set trạng thái online cho private chat
                dto.setOnline(userActivityService.isOnline(other.getId()));
            }
        } else {
            dto.setName(c.getConversationName());
            dto.setAvatar(c.getGroupAvatar());
            List<ConversationParticipant> ps = participantRepository.findByConversationId(c.getId());
            dto.setParticipantCount(ps.size());
            dto.setParticipants(ps.stream().map(this::mapParticipantDto).collect(Collectors.toList()));
            // Đối với group, kiểm tra có ít nhất 1 thành viên online không
            boolean hasOnlineMembers = ps.stream()
                    .anyMatch(p -> userActivityService.isOnline(p.getUser().getId()));
            dto.setOnline(hasOnlineMembers);
        }

        Message last = messageRepository.findFirstByConversationIdOrderBySentAtDescMessageIdDesc(c.getId());
        if (last != null) {
            dto.setLastMessage(formatLastMessage(last));
            dto.setLastMessageTime(last.getSentAt());
            dto.setTimeAgo(formatTimeAgo(last.getSentAt()));
        } else {
            dto.setLastMessage("Chưa có tin nhắn");
            dto.setTimeAgo("Vừa tạo");
        }

        Optional<ConversationParticipant> paticipant = participantRepository.findByConversationIdAndUserId(c.getId(), currentUserId);

        long unread = messageRepository.countUnreadMessages(c.getId(),paticipant.get().getLastReadMessageId());
        dto.setUnreadCount((int) unread);
        dto.setHasUnread(unread > 0);

        return dto;
    }

    private String formatLastMessage(Message message) {
        String content = Optional.ofNullable(message.getContent()).orElse("");
        String trimmed = content.length() > 50 ? content.substring(0, 50) + "..." : content;

        if (message.getConversation().getConversationType() == Conversation.ConversationType.GROUP) {
            String senderFirstName = Optional.ofNullable(message.getSender().getFirstName()).orElse("");
            String shortContent = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            return senderFirstName + ": " + shortContent;
        }
        return trimmed;
    }

    private String formatTimeAgo(LocalDateTime dt) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dt, now);
        long hours = ChronoUnit.HOURS.between(dt, now);
        long days = ChronoUnit.DAYS.between(dt, now);
        if (minutes < 1) return "Vừa xong";
        if (minutes < 60) return minutes + " phút trước";
        if (hours < 24) return hours + " giờ trước";
        return days + " ngày trước";
    }

    private String safeFullName(User u) {
        String f = Optional.ofNullable(u.getFirstName()).orElse("");
        String l = Optional.ofNullable(u.getLastName()).orElse("");
        String full = (f + " " + l).trim();
        return full.isEmpty() ? Optional.ofNullable(u.getUsername()).orElse("Người dùng") : full;
    }




}
