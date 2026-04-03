// PostDisplayDto.java
package com.codegym.socialmedia.dto.post;

import com.codegym.socialmedia.model.PrivacyLevel;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.repository.post.PostRepository;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDisplayDto {

    private Long id;
    private String content;
    private List<String> imageUrls;
    private PrivacyLevel privacyLevel;
    private PrivacyLevel privacyCommentLevel;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
    private LocalDateTime updatedAt;

    // User info
    private Long userId;
    private String username;
    private String userFullName;
    private String userAvatarUrl;

    // Stats
    private int likesCount;
    private int commentsCount;
    private boolean isLikedByCurrentUser;
    private boolean canEdit; // User có thể edit post này không
    private boolean canDelete; // User có thể delete post này không
    private boolean canComment; // User có thể comment post này không
    public PostDisplayDto(Post post, boolean isLikedByCurrentUser, boolean canEdit, boolean canDelete) {

        this.id = post.getId();
        this.content = post.getContent();
        this.imageUrls = parseImageUrls(post.getImageUrls());
        this.privacyLevel = post.getPrivacyLevel();
        this.privacyCommentLevel = post.getPrivacyCommentLevel();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();

        this.userId = post.getUser().getId();
        this.username = post.getUser().getUsername();
        this.userFullName = post.getUser().getFirstName() + " " + post.getUser().getLastName();
        this.userAvatarUrl = post.getUser().getProfilePicture();

        this.isLikedByCurrentUser = isLikedByCurrentUser;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
    }

    public PostDisplayDto(PostRepository.PostFeedProjection proj, boolean isLikedByCurrentUser, boolean canEdit, boolean canDelete) {

        this.id = proj.getId();
        this.content = proj.getContent();
        this.imageUrls = parseImageUrls(proj.getImageUrls());
        this.privacyLevel = proj.getPrivacyLevel();
        this.privacyCommentLevel = proj.getPrivacyCommentLevel();
        this.createdAt = proj.getCreatedAt();
        this.updatedAt = proj.getUpdatedAt();

        this.userId = proj.getOwnerId();
        this.username = proj.getOwnerUsername();
        this.userFullName = proj.getFullname();
        this.userAvatarUrl = proj.getOwnerAvatar();

        this.isLikedByCurrentUser = isLikedByCurrentUser;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
    }

    private List<String> parseImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.trim().isEmpty()) {
            return List.of();
        }
        // Parse JSON array string to List<String>
        try {
            // Simple JSON parsing - in production, use Jackson
            String cleaned = imageUrlsJson.replaceAll("[\\[\\]\"]", "");
            if (cleaned.trim().isEmpty()) {
                return List.of();
            }
            return List.of(cleaned.split(","));
        } catch (Exception e) {
            return List.of();
        }
    }
}