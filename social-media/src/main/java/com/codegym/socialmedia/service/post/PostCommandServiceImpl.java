package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.component.upload_file.ImageUploader;
import com.codegym.socialmedia.dto.post.PostCreateDto;
import com.codegym.socialmedia.dto.post.PostUpdateDto;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.repository.post.PostRepository;
import com.codegym.socialmedia.service.RedisFeedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class PostCommandServiceImpl implements PostCommandService {

    private final PostRepository postRepository;
    private final ImageUploader imageUploader;
    private final ObjectMapper objectMapper;
    private final RedisFeedService redisFeedService;

    public Post createPost(PostCreateDto dto, User user) {
        Post post = new Post();
        post.setUser(user);
        post.setContent(dto.getContent());
        post.setPrivacyLevel(dto.getPrivacyLevel());

        post.setImageUrls(uploadImages(dto.getImages()));

        Post saved = postRepository.save(post);

        // feed chỉ là side-effect
        redisFeedService.pushToFeed(
                user.getId(),
                saved.getId(),
                saved.getCreatedAt().toEpochSecond(ZoneOffset.UTC)
        );

        return saved;
    }

    public Post updatePost(Long postId, PostUpdateDto dto, User user) {
        Post post = postRepository.findByIdAndUser(postId, user)
                .orElseThrow();

        post.setContent(dto.getContent());
        post.setPrivacyLevel(dto.getPrivacyLevel());
        post.setPrivacyCommentLevel(dto.getCommentPrivacyLevel());

        post.setImageUrls(uploadUpdatedImages(dto));
        return postRepository.save(post);
    }

    public void deletePost(Long postId, User user) {
        Post post = postRepository.findByIdAndUser(postId, user)
                .orElseThrow();
        post.setDeleted(true);
    }

    // helpers
    private String uploadImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return "[]";

        List<String> urls = images.stream()
                .filter(f -> !f.isEmpty())
                .map(imageUploader::upload)
                .filter(Objects::nonNull)
                .toList();

        try {
            return objectMapper.writeValueAsString(urls);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String uploadUpdatedImages(PostUpdateDto dto) {
        List<String> result = new ArrayList<>();

        if (dto.getExistingImages() != null)
            result.addAll(dto.getExistingImages());

        if (dto.getImagesToDelete() != null)
            result.removeAll(dto.getImagesToDelete());

        if (dto.getNewImages() != null) {
            dto.getNewImages().stream()
                    .filter(f -> !f.isEmpty())
                    .map(imageUploader::upload)
                    .filter(Objects::nonNull)
                    .forEach(result::add);
        }

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "[]";
        }
    }
}
