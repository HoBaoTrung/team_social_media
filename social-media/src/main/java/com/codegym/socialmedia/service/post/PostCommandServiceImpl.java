package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.component.upload_file.ImageUploader;
import com.codegym.socialmedia.dto.post.BasePrivacyDto;
import com.codegym.socialmedia.dto.post.PostCreateDto;
import com.codegym.socialmedia.dto.post.PostUpdateDto;
import com.codegym.socialmedia.model.PrivacyLevel;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.model.social_action.PostPrivacyUser;
import com.codegym.socialmedia.repository.post.PostPrivacyUserRepository;
import com.codegym.socialmedia.repository.post.PostRepository;
import com.codegym.socialmedia.service.redis.RedisFeedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCommandServiceImpl implements PostCommandService {

    private final PostRepository postRepository;
    private final ImageUploader imageUploader;
    private final ObjectMapper objectMapper;
    private final PostPrivacyUserRepository postPrivacyUserRepository;
    private final RedisFeedService redisFeedService;
    private final EntityManager entityManager;

    @Override
    public Post createPost(PostCreateDto dto, User owner) {

        Post post = new Post();
        post.setUser(owner);
        post.setContent(dto.getContent());
        post.setPrivacyLevel(dto.getPrivacyLevel());
        post.setImageUrls(uploadImages(dto.getImages()));

        postRepository.save(post);

        updatePrivacyUsers(post, dto);

        redisFeedService.fanOutPost(post);

        return post;
    }

    @Override
    public Post updatePost(Long postId, PostUpdateDto dto, User owner) {

        Post post = postRepository.findByIdAndUser(postId, owner)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        post.setContent(dto.getContent());
        post.setPrivacyLevel(dto.getPrivacyLevel());
        post.setPrivacyCommentLevel(dto.getCommentPrivacyLevel());
        post.setImageUrls(uploadUpdatedImages(dto));

        updatePrivacyUsers(post, dto);
        redisFeedService.fanOutPost(post);
        return post;
    }

    private void updatePrivacyUsers(Post post, BasePrivacyDto dto) {

        // Xóa toàn bộ privacy cũ
        postPrivacyUserRepository.deleteByPostId(post.getId());

        PrivacyLevel level = dto.getPrivacyLevel();

        if (level != PrivacyLevel.FRIEND_EXCEPT &&
                level != PrivacyLevel.SPECIFIC_FRIENDS) {
            return;
        }

        Set<Long> targetUserIds =
                level == PrivacyLevel.FRIEND_EXCEPT
                        ? safeSet(dto.getExcludedUserIds())
                        : safeSet(dto.getAllowedUserIds());

        boolean isAccept = level == PrivacyLevel.SPECIFIC_FRIENDS;

        if (targetUserIds.isEmpty()) return;

        List<PostPrivacyUser> privacyUsers = targetUserIds.stream()
                .distinct()
                .map(userId -> {

                    PostPrivacyUser relation = new PostPrivacyUser();
                    relation.setPost(post);
                    relation.setUser(entityManager.getReference(User.class, userId));
                    relation.setAccept(isAccept);
                    return relation;
                })
                .toList();

        postPrivacyUserRepository.saveAll(privacyUsers);
    }

    private Set<Long> safeSet(Set<Long> input) {
        return input == null ? Collections.emptySet() : input;
    }


    @Override
    public void deletePost(Long postId, User owner) {

        Post post = postRepository.findByIdAndUser(postId, owner)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        post.setDeleted(true);
        postPrivacyUserRepository.deleteByPostId(post.getId());

    }


    // helpers
    private String uploadImages(List<MultipartFile> images) {

        if (images == null || images.isEmpty()) return "[]";

        List<String> urls = images.stream()
                .filter(f -> f != null && !f.isEmpty())
                .map(imageUploader::upload)
                .filter(Objects::nonNull)
                .toList();

        return toJson(urls);
    }

    private String uploadUpdatedImages(PostUpdateDto dto) {

        List<String> result = new ArrayList<>();

        if (dto.getExistingImages() != null)
            result.addAll(dto.getExistingImages());

        if (dto.getImagesToDelete() != null)
            result.removeAll(dto.getImagesToDelete());

        if (dto.getNewImages() != null) {
            dto.getNewImages().stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .map(imageUploader::upload)
                    .filter(Objects::nonNull)
                    .forEach(result::add);
        }

        return toJson(result);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to JSON", e);
        }
    }
}
