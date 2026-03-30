package com.codegym.socialmedia.service.firestore;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Firestore Chat Service
 *
 * Handles all Firestore operations for real-time messaging:
 * - Send messages (text, image, file)
 * - Update conversation metadata
 * - Manage unread counts and read receipts
 * - Create/fetch conversations
 */
@Slf4j
@Service
@AllArgsConstructor
public class FirestoreChatService {

    private final Firestore firestore;

    private static final String CONVERSATIONS_COLLECTION = "conversations";
    private static final String MESSAGES_SUBCOLLECTION = "messages";
    private static final long MESSAGE_SIZE_LIMIT = 1000; // Max 1000 chars per message


    /**
     * Send a message to a conversation
     *
     * @param conversationId Conversation where message is sent
     * @param senderId User sending the message
     * @param messageData Message data (content, type, fileUrl, etc.)
     * @return CompletableFuture with document ID of created message
     */
    @Async
    public CompletableFuture<String> sendMessage(String conversationId, Long senderId, Map<String, Object> messageData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate inputs
                if (conversationId == null || conversationId.isEmpty()) {
                    throw new IllegalArgumentException("Conversation ID cannot be null");
                }
                if (senderId == null) {
                    throw new IllegalArgumentException("Sender ID cannot be null");
                }

                String content = (String) messageData.getOrDefault("content", "");
                if (content.length() > MESSAGE_SIZE_LIMIT) {
                    throw new IllegalArgumentException("Message exceeds size limit (" + MESSAGE_SIZE_LIMIT + " chars)");
                }

                // Prepare message object
                Map<String, Object> messageMap = new HashMap<>(messageData);
                messageMap.put("senderId", senderId);
                messageMap.put("createdAt", com.google.cloud.Timestamp.now());
                messageMap.put("type", messageMap.getOrDefault("type", "TEXT"));

                // Write message to Firestore
                DocumentReference newMessage = firestore
                    .collection(CONVERSATIONS_COLLECTION)
                    .document(conversationId)
                    .collection(MESSAGES_SUBCOLLECTION)
                    .add(messageMap)
                    .get();

                String messageId = newMessage.getId();
                log.info("✅ Message sent: conversationId={}, messageId={}, senderId={}",
                    conversationId, messageId, senderId);

                // Update conversation metadata asynchronously (non-blocking)
                updateConversationMetadata(conversationId, content, senderId)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("⚠️ Failed to update conversation metadata: {}", ex.getMessage());
                        }
                    });

                return messageId;

            } catch (Exception e) {
                log.error("❌ Error sending message: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to send message: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Update conversation metadata after message is sent
     *
     * @param conversationId Conversation ID
     * @param lastMessage Last message text
     * @param senderId User who sent the message
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> updateConversationMetadata(String conversationId, String lastMessage, Long senderId) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Prepare update data
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("lastMessage", lastMessage.length() > 100 ? lastMessage.substring(0, 100) + "..." : lastMessage);
                updateData.put("updatedAt", com.google.cloud.Timestamp.now());

                // Get conversation document to find all participants
                DocumentSnapshot convDoc = firestore
                    .collection(CONVERSATIONS_COLLECTION)
                    .document(conversationId)
                    .get()
                    .get();

                if (convDoc.exists()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Boolean> participants = (Map<String, Boolean>) convDoc.get("participants");

                    if (participants != null) {
                        // Increment unread count for all participants except sender
                        Map<String, Object> unreadCounts = new HashMap<>();
                        for (String userId : participants.keySet()) {
                            long unreadId = Long.parseLong(userId);
                            if ( !(unreadId==senderId) ) {
                                // Increment unread count
                                Long currentUnread = convDoc.getLong("unreadCount." + userId) != null
                                    ? convDoc.getLong("unreadCount." + userId)
                                    : 0L;
                                unreadCounts.put("unreadCount." + userId, currentUnread + 1);
                            } else {
                                // Reset unread for sender (they can see their own message)
                                unreadCounts.put("unreadCount." + userId, 0L);
                            }
                        }
                        updateData.putAll(unreadCounts);
                    }
                }

                // Apply update
                firestore
                    .collection(CONVERSATIONS_COLLECTION)
                    .document(conversationId)
                    .update(updateData)
                    .get();

                log.info("✅ Conversation metadata updated: conversationId={}", conversationId);

            } catch (Exception e) {
                log.warn("⚠️ Failed to update conversation metadata: {}", e.getMessage());
                // Don't throw - this is non-critical
            }
        });
    }

    /**
     * Create a new conversation or get existing one
     *
     * @param participantIds List of user IDs in conversation
     * @return CompletableFuture with conversation ID
     */
    @Async
    public CompletableFuture<String> createOrGetConversation(List<Long> participantIds) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Sort IDs for consistent conversation naming
                String conversationId = participantIds.stream()
                    .map(String::valueOf)
                    .sorted()
                    .collect(Collectors.joining("_"));

                DocumentReference convRef = firestore
                    .collection(CONVERSATIONS_COLLECTION)
                    .document(conversationId);

                DocumentSnapshot doc = convRef.get().get();

                if (!doc.exists()) {
                    // Create new conversation
                    Map<String, Object> convData = new HashMap<>();
                    Map<String, Boolean> participantMap = new HashMap<>();
                    for (Long id : participantIds) {
                        participantMap.put(String.valueOf(id), true);
                    }
                    convData.put("participants", participantMap);
                    convData.put("createdAt", com.google.cloud.Timestamp.now());
                    convData.put("updatedAt", com.google.cloud.Timestamp.now());

                    // Initialize unread counts
                    Map<String, Long> unreadCounts = new HashMap<>();
                    for (Long id : participantIds) {
                        unreadCounts.put(String.valueOf(id), 0L);
                    }
                    convData.put("unreadCount", unreadCounts);

                    // Initialize lastRead
                    Map<String, Object> lastRead = new HashMap<>();
                    for (Long id : participantIds) {
                        lastRead.put(String.valueOf(id), com.google.cloud.Timestamp.now());
                    }
                    convData.put("lastRead", lastRead);

                    convRef.set(convData).get();
                    log.info("✅ New conversation created: {}", conversationId);
                } else {
                    log.info("ℹ️ Using existing conversation: {}", conversationId);
                }

                return conversationId;

            } catch (Exception e) {
                log.error("❌ Error creating/getting conversation: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to create conversation: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Reset unread count for a user in a conversation
     *
     * @param conversationId Conversation ID
     * @param userId User ID
     * @return CompletableFuture<Void>
     */
    @Async
    public CompletableFuture<Void> resetUnreadCount(String conversationId, Long userId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("unreadCount." + userId, 0L);
                updateData.put("lastRead." + userId, com.google.cloud.Timestamp.now());

                firestore
                    .collection(CONVERSATIONS_COLLECTION)
                    .document(conversationId)
                    .update(updateData)
                    .get();

                log.info("✅ Unread count reset: conversationId={}, userId={}", conversationId, userId);

            } catch (Exception e) {
                log.warn("⚠️ Failed to reset unread count: {}", e.getMessage());
            }
        });
    }

    /**
     * Get unread count for a user in a conversation
     *
     * @param conversationId Conversation ID
     * @param userId User ID
     * @return Unread count
     */
    public Long getUnreadCount(String conversationId, Long userId) {
        try {
            DocumentSnapshot doc = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .get()
                .get();

            if (doc.exists()) {
                Long unread = doc.getLong("unreadCount." + userId);
                return unread != null ? unread : 0L;
            }
            return 0L;

        } catch (Exception e) {
            log.warn("⚠️ Failed to get unread count: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Get all messages in a conversation (paginated)
     *
     * @param conversationId Conversation ID
     * @param limit Number of messages to fetch
     * @return List of message documents
     */
    public CompletableFuture<List<Map<String, Object>>> getMessages(String conversationId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Query query = firestore
                    .collection(CONVERSATIONS_COLLECTION)
                    .document(conversationId)
                    .collection(MESSAGES_SUBCOLLECTION)
                    .orderBy("createdAt", com.google.cloud.firestore.Query.Direction.DESCENDING)
                    .limit(limit);

                QuerySnapshot snapshot = query.get().get();

                return snapshot.getDocuments().stream()
                    .map(doc -> {
                        Map<String, Object> data = doc.getData();
                        data.put("id", doc.getId());
                        return data;
                    })
                    .sorted(Comparator.comparing(
                        m -> ((com.google.cloud.Timestamp) m.get("createdAt")).toDate()
                    ))
                    .collect(Collectors.toList());

            } catch (Exception e) {
                log.error("❌ Error fetching messages: {}", e.getMessage(), e);
                return List.of();
            }
        });
    }

    /**
     * Delete a conversation (soft delete - just mark inactive)
     *
     * Note: Hard deletion is not recommended as it breaks message history.
     * Instead, remove user from participants list.
     *
     * @param conversationId Conversation ID
     * @param userId User to remove
     * @return CompletableFuture<Void>
     */
//    @Async
//    public CompletableFuture<Void> removeUserFromConversation(String conversationId, Long userId) {
//        return CompletableFuture.runAsync(() -> {
//            try {
//                Map<String, Object> updateData = new HashMap<>();
//                updateData.put("participants." + userId, new com.google.firestore.v1.DocumentTransform.FieldTransform.ArrayUnion());
//
//                firestore
//                    .collection(CONVERSATIONS_COLLECTION)
//                    .document(conversationId)
//                    .update(updateData)
//                    .get();
//
//                log.info("✅ User removed from conversation: conversationId={}, userId={}", conversationId, userId);
//
//            } catch (Exception e) {
//                log.warn("⚠️ Failed to remove user from conversation: {}", e.getMessage());
//            }
//        });
//    }

    /**
     * Check if user is participant of a conversation
     *
     * @param conversationId Conversation ID
     * @param userId User ID
     * @return true if user is participant
     */
    public boolean isUserParticipant(String conversationId, Long userId) {
        try {
            DocumentSnapshot doc = firestore
                .collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .get()
                .get();

            if (doc.exists()) {
                @SuppressWarnings("unchecked")
                Map<String, Boolean> participants = (Map<String, Boolean>) doc.get("participants");
                return participants != null && participants.getOrDefault(String.valueOf(userId), false);
            }
            return false;

        } catch (Exception e) {
            log.warn("⚠️ Failed to check participant status: {}", e.getMessage());
            return false;
        }
    }
}
