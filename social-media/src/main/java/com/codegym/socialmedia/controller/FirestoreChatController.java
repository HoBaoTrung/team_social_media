package com.codegym.socialmedia.controller;

import com.codegym.socialmedia.service.firestore.FileUploadService;
import com.codegym.socialmedia.service.firestore.FileUploadService.FileUploadResult;
import com.codegym.socialmedia.service.firestore.FirestoreChatService;
import com.codegym.socialmedia.service.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Firestore Chat REST Controller
 *
 * Handles all Firestore chat operations:
 * - Send messages to Firestore
 * - Upload files (saved to local backend storage)
 * - Mark conversations as read
 * - Create conversations
 *
 * All endpoints require authentication (JWT token from /api/auth/login)
 */
@Slf4j
@RestController
@RequestMapping("/api/firestore/chat")
@CrossOrigin(origins = "http://localhost:4200")
public class FirestoreChatController {

    @Autowired
    private FirestoreChatService firestoreChatService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private UserService userService;

    /**
     * Send a message to Firestore
     *
     * Endpoint: POST /api/firestore/chat/send-message
     * Body: {
     *   "conversationId": "string",
     *   "content": "string",
     *   "type": "TEXT|IMAGE|FILE"
     * }
     *
     * @param request Message request
     * @param userId Authenticated user ID (from JWT token)
     * @return Message creation response
     */
    @PostMapping("/send-message")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestBody Map<String, Object> request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get authenticated user ID
            // Note: userDetails might be null in JWT context, use userService instead
            Long senderId = userService.getCurrentUser().getId();

            String conversationId = (String) request.get("conversationId");
            String content = (String) request.get("content");
            String type = (String) request.getOrDefault("type", "TEXT");
            String fileUrl = (String) request.getOrDefault("fileUrl", "");

            // Validate input
            if (conversationId == null || conversationId.isEmpty()) {
                response.put("success", false);
                response.put("error", "Conversation ID is required");
                return ResponseEntity.badRequest().body(response);
            }

            if (content == null || content.isEmpty()) {
                response.put("success", false);
                response.put("error", "Message content cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if user is participant of conversation
            if (!firestoreChatService.isUserParticipant(conversationId, senderId)) {
                response.put("success", false);
                response.put("error", "User is not a participant of this conversation");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Prepare message data
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("content", content);
            messageData.put("type", type);
            messageData.put("receiverId", request.getOrDefault("receiverId", ""));
            if (!fileUrl.isEmpty()) {
                messageData.put("fileUrl", fileUrl);
                messageData.put("fileName", request.getOrDefault("fileName", ""));
            }

            // Send message async
            CompletableFuture<String> messageFuture = firestoreChatService
                .sendMessage(conversationId, senderId, messageData);

            // Get result with timeout
            String messageId = messageFuture.get(
                java.util.concurrent.TimeUnit.SECONDS.toNanos(5),
                java.util.concurrent.TimeUnit.NANOSECONDS
            );

            response.put("success", true);
            response.put("messageId", messageId);
            response.put("timestamp", System.currentTimeMillis());

            log.info("✅ Message sent successfully: {}", messageId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid message: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("❌ Error sending message: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to send message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Upload file for chat message
     *
     * Endpoint: POST /api/firestore/chat/upload-file
     * Params: file (multipart), type (image|document)
     *
     * @param file File to upload
     * @param type File type: 'image' or 'document'
     * @return FileUploadResult with fileUrl
     */
    @PostMapping("/upload-file")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "document") String type) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = userService.getCurrentUser().getId();

            // Validate file type parameter
            if (!Arrays.asList("image", "document").contains(type.toLowerCase())) {
                response.put("success", false);
                response.put("error", "File type must be 'image' or 'document'");
                return ResponseEntity.badRequest().body(response);
            }

            // Upload file
            FileUploadResult result = fileUploadService.uploadFile(file, userId, type);

            response.put("success", true);
            response.putAll(Map.of(
                "fileUrl", result.getFileUrl(),
                "fileName", result.getFileName(),
                "fileType", result.getFileType(),
                "fileSize", result.getFileSize()
            ));

            log.info("✅ File uploaded: {} → {}", result.getFileName(), result.getFileUrl());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid file: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("❌ Error uploading file: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Mark conversation as read
     *
     * Endpoint: PUT /api/firestore/chat/mark-read/{conversationId}
     *
     * @param conversationId Conversation to mark as read
     * @return Success/error response
     */
    @PutMapping("/mark-read/{conversationId}")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable String conversationId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = userService.getCurrentUser().getId();

            // Check if user is participant
            if (!firestoreChatService.isUserParticipant(conversationId, userId)) {
                response.put("success", false);
                response.put("error", "User is not a participant of this conversation");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Reset unread count async
            firestoreChatService.resetUnreadCount(conversationId, userId)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("⚠️ Error resetting unread count: {}", ex.getMessage());
                    } else {
                        log.info("✅ Conversation marked as read: {}", conversationId);
                    }
                });

            response.put("success", true);
            response.put("message", "Conversation marked as read");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error marking conversation as read: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to mark as read: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get unread count for a conversation
     *
     * Endpoint: GET /api/firestore/chat/unread-count/{conversationId}
     *
     * @param conversationId Conversation ID
     * @return Unread count
     */
    @GetMapping("/unread-count/{conversationId}")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable String conversationId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = userService.getCurrentUser().getId();

            // Check if user is participant
            if (!firestoreChatService.isUserParticipant(conversationId, userId)) {
                response.put("success", false);
                response.put("error", "User is not a participant of this conversation");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            long unreadCount = firestoreChatService.getUnreadCount(conversationId, userId);

            response.put("success", true);
            response.put("conversationId", conversationId);
            response.put("unreadCount", unreadCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting unread count: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to get unread count: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Create a new conversation or get existing one
     *
     * Endpoint: POST /api/firestore/chat/create-conversation
     * Body: { "participantIds": [1, 2, ...] }
     *
     * @param request Create conversation request
     * @return Created conversation ID
     */
    @PostMapping("/create-conversation")
    public ResponseEntity<Map<String, Object>> createConversation(
            @RequestBody Map<String, List<Long>> request) {

        Map<String, Object> response = new HashMap<>();

        try {
            List<Long> participantIds = request.get("participantIds");

            if (participantIds == null || participantIds.isEmpty()) {
                response.put("success", false);
                response.put("error", "Participant IDs are required");
                return ResponseEntity.badRequest().body(response);
            }

            // Create conversation async
            CompletableFuture<String> convFuture = firestoreChatService
                .createOrGetConversation(participantIds);

            // Get result with timeout
            String conversationId = convFuture.get(
                java.util.concurrent.TimeUnit.SECONDS.toNanos(5),
                java.util.concurrent.TimeUnit.NANOSECONDS
            );

            response.put("success", true);
            response.put("conversationId", conversationId);

            log.info("✅ Conversation created: {} with participants: {}", conversationId, participantIds);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error creating conversation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", "Failed to create conversation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
