package com.codegym.socialmedia.controller;

import com.codegym.socialmedia.dto.chat.*;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.conversation.Conversation;
import com.codegym.socialmedia.model.conversation.ConversationParticipant;
import com.codegym.socialmedia.repository.ConversationParticipantRepository;
import com.codegym.socialmedia.repository.ConversationRepository;
import com.codegym.socialmedia.repository.IUserRepository;
import com.codegym.socialmedia.service.chat.ChatService;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import com.codegym.socialmedia.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.*;

@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private UserService userService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private IUserRepository userRepository;

    @Autowired private ConversationParticipantRepository participantRepository;

    @PostMapping("/api/chat/search-users-for-group")
    @ResponseBody
    public ResponseEntity<List<UserSearchDto>> searchUsersForGroup(@RequestParam("query") String q) {
        Long me = userService.getCurrentUser().getId();
        // Chỉ tìm bạn bè để tạo nhóm
        List<UserSearchDto> friends = friendshipService.searchFriends(q, me);
        return ResponseEntity.ok(friends);
    }

    // THÊM method này vào ChatController.java
    @GetMapping("/api/chat/search-friends")
    @ResponseBody
    public ResponseEntity<List<UserSearchDto>> searchFriends(@RequestParam("query") String q) {
        Long me = userService.getCurrentUser().getId();
        List<UserSearchDto> friends = friendshipService.searchFriends(q, me);
        return ResponseEntity.ok(friends);
    }

    @PostMapping("/api/chat/find-or-create-conversation")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> findOrCreateConversation(@RequestBody Map<String, Long> request) {
        Map<String, Object> res = new HashMap<>();
        try {
            Long targetUserId = request.get("targetUserId");
            if (targetUserId == null) {
                res.put("success", false);
                res.put("error", "Target user ID is required");
                return ResponseEntity.badRequest().body(res);
            }
            Long me = userService.getCurrentUser().getId();
            ConversationDto conversation = chatService.findOrCreatePrivateConversation(me, targetUserId);
            res.put("success", true);
            res.put("conversation", conversation);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // API lấy danh sách cuộc trò chuyện cho dropdown
    @GetMapping("/api/conversations")
    @ResponseBody
    public ResponseEntity<List<ConversationDto>> getConversations() {
        Long me = userService.getCurrentUser().getId();
        return ResponseEntity.ok(chatService.getConversationsForUser(me));
    }

    // API lấy bạn bè online cho sidebar
    @GetMapping("/api/chat/online-friends")
    @ResponseBody
    public ResponseEntity<List<ConversationDto>> getOnlineFriends() {
        Long me = userService.getCurrentUser().getId();
        return ResponseEntity.ok(chatService.getOnlineFriends(me));
    }

    // bạn bè + nhóm của tôi
    @GetMapping("/api/chat/contacts")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getContacts() {
        Long me = userService.getCurrentUser().getId();
        Map<String, Object> res = new HashMap<>();

        // Lấy bạn bè online
        List<ConversationDto> onlineFriends = chatService.getOnlineFriends(me);

        // Lấy groups với thông tin đầy đủ
        List<ConversationDto> groups = chatService.getGroupsForUser(me);

        res.put("friends", onlineFriends);
        res.put("groups", groups);

        return ResponseEntity.ok(res);
    }

    @GetMapping("/api/chat/search-users")
    @ResponseBody
    public ResponseEntity<List<UserSearchDto>> searchUsers(@RequestParam("query") String q) {
        Long me = userService.getCurrentUser().getId();
        return ResponseEntity.ok(chatService.searchUsers(q, me));
    }

    // API gửi tin nhắn
    @PostMapping("/api/chat/send-message")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam("conversationId") Long conversationId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        Map<String, Object> res = new HashMap<>();
        try {
            Long me = userService.getCurrentUser().getId();

            List<AttachmentDto> attachments = new ArrayList<>();
            if (files != null && !files.isEmpty()) {
                String uploadDir = System.getProperty("user.dir") + "/uploads/";
                File uploadFolder = new File(uploadDir);
                if (!uploadFolder.exists()) {
                    if (!uploadFolder.mkdirs()) {
                        throw new RuntimeException("Failed to create upload directory");
                    }
                }
                long totalSize = 0;
                for (MultipartFile file : files) {
                    if (file.isEmpty()) continue;
                    long fileSize = file.getSize();
                    if (fileSize > 50 * 1024 * 1024) {  // Tăng lên 50MB per file
                        throw new RuntimeException("File too large: " + file.getOriginalFilename() + " (max 50MB)");
                    }
                    totalSize += fileSize;
                    if (totalSize > 200 * 1024 * 1024) {  // Giới hạn tổng 200MB cho multiple files để tránh overload
                        throw new RuntimeException("Total files size too large (max 200MB)");
                    }

                    String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
                    String safeFileName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-\\_]", "_");
                    String fileName = UUID.randomUUID() + "_" + safeFileName;
                    Path path = Paths.get(uploadDir, fileName);
                    Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                    String attachmentUrl = "/uploads/" + fileName;

                    AttachmentDto dto = new AttachmentDto();
                    dto.setAttachmentUrl(attachmentUrl);
                    dto.setFileName(file.getOriginalFilename());
                    dto.setFileSize(file.getSize());
                    dto.setType(getFileType(file.getContentType()));
                    attachments.add(dto);
                }
            }

            SendMessageRequest request = new SendMessageRequest();
            request.setConversationId(conversationId);
            request.setContent(content);
            request.setAttachments(attachments);

            MessageDto msg = chatService.sendMessage(me, request);

            messagingTemplate.convertAndSend("/topic/conversation/" + msg.getConversationId(), msg);

            List<ConversationParticipant> participants =
                    participantRepository.findByConversationId(conversationId);
            for (ConversationParticipant p : participants) {
                Long pid = p.getUser().getId();
                if (!pid.equals(me)) {
                    long totalUnread = chatService.getTotalUnread(pid);
                    long unreadCount = chatService.getUnreadCount(conversationId, pid);
                    String username = p.getUser().getUsername();
                    messagingTemplate.convertAndSendToUser(
                            username,
                            "/queue/unread",
                            new UnreadNotification(conversationId, unreadCount, totalUnread)
                    );
                }
            }
            res.put("success", true);
            res.put("message", msg);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);  // Changed to badRequest for client errors
        }
    }
    private String getFileType(String contentType) {
        if (contentType == null) return "FILE";
        if (contentType.startsWith("image")) return "IMAGE";
        if (contentType.startsWith("video")) return "VIDEO";
        if (contentType.startsWith("audio")) return "AUDIO";
        return "FILE";
    }

    @GetMapping("/unread/{conversationId}/{userId}")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long conversationId,
                                               @PathVariable Long userId) {
        return ResponseEntity.ok(chatService.getUnreadCount(conversationId, userId));
    }

    @GetMapping("/api/chat/unread/total/{userId}")
    public ResponseEntity<Long> getTotalUnread(@PathVariable Long userId) {
        return ResponseEntity.ok(chatService.getTotalUnread(userId));
    }

    @PostMapping("/api/chat/mark-read/{conversationId}/{userId}")
    public ResponseEntity<Void> markAsRead(@PathVariable Long conversationId,
                                           @PathVariable Long userId) {
        chatService.markAsRead(conversationId, userId);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/api/chat/conversation/{id}/messages")
    @ResponseBody
    public ResponseEntity<List<MessageDto>> getMessagesByConversation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.getMessagesByConversation(id, page, size));
    }

    @GetMapping("/api/chat/conversation/{id}/participants")
    @ResponseBody
    public ResponseEntity<List<ConversationDto.ParticipantDto>> getParticipants(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.getParticipants(id));
    }

    @PostMapping("/api/chat/conversation/{id}/avatar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateGroupAvatar(
            @PathVariable Long id,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar) {
        Map<String, Object> res = new HashMap<>();
        try {
            String url = chatService.updateGroupAvatar(id, avatar);
            res.put("success", true);
            res.put("avatarUrl", url);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @PostMapping("/api/chat/create-group-from-ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createGroupFromIds(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        try {
            User me = userService.getCurrentUser();
            String groupName = (String) body.getOrDefault("groupName", "Nhóm mới");
            @SuppressWarnings("unchecked")
            List<Integer> raw = (List<Integer>) body.get("participantIds");
            List<Long> ids = (raw == null) ? List.of() : raw.stream().map(Long::valueOf).toList();

            ConversationDto conv = chatService.createGroupFromIds(me.getId(), ids, groupName);
            res.put("success", true);
            res.put("conversation", conv);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("success", false);
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        }
    }

    @GetMapping("/api/chat/history")
    @ResponseBody
    public ResponseEntity<List<MessageDto>> getChatHistory(@RequestParam Long userId1, @RequestParam Long userId2) {
        return ResponseEntity.ok(chatService.getChatHistory(userId1, userId2));
    }


    @Autowired
    private ConversationParticipantRepository conversationParticipantRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @GetMapping("/video_call/{conversationId}")
    public String showVideoCall(@PathVariable Long conversationId,
                                @RequestParam(required = false) Boolean isIncoming,
                                @RequestParam(required = false) Long callerId,
                                Model model) {
        User currentUser = userService.getCurrentUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation không tồn tại"));

        ConversationParticipant participant =
                conversationParticipantRepository.findByConversationIdAndUserId(conversationId, currentUser.getId()) .orElseThrow(() -> new RuntimeException("Not participant"));;

        if (participant == null) {
            throw new RuntimeException("Bạn không có quyền vào cuộc gọi này");
        }

        boolean isCaller = (isIncoming == null || !isIncoming);  // Nếu không có isIncoming, là caller
        boolean incoming = !isCaller;
        model.addAttribute("isCaller", isCaller);
        model.addAttribute("isIncoming", incoming);

        // Find remote info
        String remoteName;
        String remoteAvatar;
        if (conversation.getParticipants().size() > 2) {
            // Group call
            remoteName = conversation.getConversationName() != null ? conversation.getConversationName() : "Cuộc gọi nhóm";
            remoteAvatar = conversation.getGroupAvatar(); // Default group avatar
        } else {
            // 1-1: Find the other participant
            User remoteUser;
            if (isCaller) {
                remoteUser = conversation.getParticipants().stream()
                        .filter(p -> !p.getUser().getId().equals(currentUser.getId()))
                        .findFirst().map(ConversationParticipant::getUser)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người nhận"));
            } else {
                remoteUser = userService.getUserById(callerId); // Caller is remote for callee
            }
            remoteName = remoteUser.getFirstName() + " " + remoteUser.getLastName();
            remoteAvatar = remoteUser.getProfilePicture();
        }
        model.addAttribute("participantsCount", conversation.getParticipants().size());
        model.addAttribute("conversationId", conversationId);
        model.addAttribute("userId", currentUser.getId());
        model.addAttribute("remoteName", remoteName);
        model.addAttribute("remoteAvatar", remoteAvatar);
        return "video_call/index";
    }


    @MessageMapping("/endCall")
    public void endCall(RejectPayload signal) {
        Long conversationId = signal.getConversationId();
        Long rejecterId = signal.getRejecterId();
        // Lấy list participant từ conversation
        List<User> participants = conversationParticipantRepository.findUsersByConversationId(conversationId);
        messagingTemplate.convertAndSend(
                "/topic/video/" + conversationId,
                new SignalMessage("leave", rejecterId, null, null)
        );
        if (participants.size() == 2) {  // Call 1-1: Gửi đến tất cả để đóng
            for (User u : participants) {
                messagingTemplate.convertAndSendToUser(
                        u.getUsername(),
                        "/queue/call-end",
                        signal
                );
            }
        } else {  // Call nhóm: Chỉ gửi đến rejecter để chỉ họ thoát
            User rejecter = participants.stream()
                    .filter(u -> u.getId().equals(rejecterId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Rejecter not found"));
            messagingTemplate.convertAndSendToUser(
                    rejecter.getUsername(),
                    "/queue/call-end",
                    signal
            );
        }
    }

    @MessageMapping("/everyoneMention")
    public void handleEveryoneMention(SimpMessageHeaderAccessor headerAccessor, @Payload Map<String, Object> payload) {
        User sender = getCurrentUser(headerAccessor);
        Long conversationId = Long.valueOf(payload.get("conversationId").toString());
        Long participantId = Long.valueOf(payload.get("participantId").toString());
        String message = payload.get("message").toString();
        String senderName = payload.get("senderName").toString();

        try {
            // Verify sender is in conversation
            boolean senderInConversation = conversationParticipantRepository
                    .findByConversationIdAndUserId(conversationId, sender.getId())
                    .isPresent();

            if (!senderInConversation) {
                return; // Unauthorized
            }

            // Get participant user
            User participant = userRepository.findById(participantId)
                    .orElse(null);

            if (participant == null) {
                return;
            }

            // Verify participant is in conversation
            boolean participantInConversation = conversationParticipantRepository
                    .findByConversationIdAndUserId(conversationId, participantId)
                    .isPresent();

            if (!participantInConversation) {
                return;
            }

            // Get conversation details for the notification
            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElse(null);

            if (conversation == null) {
                return;
            }

            // Send notification to specific user to open chat
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("type", "EVERYONE_MENTION");
            notificationData.put("conversationId", conversationId);
            notificationData.put("conversationName", conversation.getConversationName());
            notificationData.put("senderName", senderName);
            notificationData.put("message", message);
            notificationData.put("senderAvatar", sender.getProfilePicture());

            messagingTemplate.convertAndSendToUser(
                    participant.getUsername(),
                    "/queue/everyone-mention",
                    notificationData
            );

        } catch (Exception e) {
            System.err.println("Error handling @everyone mention: " + e.getMessage());
        }
    }


    @MessageMapping("/video/{conversationId}")
    @SendTo("/topic/video/{conversationId}")
    public SignalMessage sendSignal(@DestinationVariable Long conversationId, SignalMessage message) {
        return message;
    }

    private User getCurrentUser(SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (!(principal instanceof Authentication)) {
            throw new RuntimeException("Invalid principal type");
        }
        Authentication auth = (Authentication) principal;


        if (auth == null) {
            throw new RuntimeException("User not authenticated");
        }

        return getUserFromAuthentication(auth);
    }

    @MessageMapping("/startCall")
    public void startCall(SimpMessageHeaderAccessor headerAccessor, @Payload VideoCallInvite videoCallInvite) {

        User caller = getCurrentUser(headerAccessor);
        Conversation conv = conversationParticipantRepository.findByIdWithParticipants(videoCallInvite.getConversationId())
                .orElseThrow();

        long callerId = caller.getId();
        for (ConversationParticipant p : conv.getParticipants()) {
            if (!p.getUser().getId().equals(callerId)) {
                String username = p.getUser().getUsername();
                messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/call-invite",
                        new VideoCallInvite(caller.getId(), conv.getId(),
                                caller.getFirstName() + " " + caller.getLastName(), caller.getProfilePicture())
                );
            }
        }
    }

    private User getUserFromAuthentication(Authentication auth) {
        Object principal = auth.getPrincipal();

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return userService.getUserByUsername(username);
        } else if (principal instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = oauth2User.getAttribute("email");
            return userService.findByEmail(email);
        }

        throw new RuntimeException("Unsupported authentication type");
    }

}