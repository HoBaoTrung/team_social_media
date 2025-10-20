package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.component.CloudinaryService;
import com.codegym.socialmedia.component.PrivacyUtils;
import com.codegym.socialmedia.dto.post.PostCreateDto;
import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.dto.post.PostUpdateDto;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.*;
import com.codegym.socialmedia.repository.user.IUserRepository;
import com.codegym.socialmedia.repository.post.PostCommentRepository;
import com.codegym.socialmedia.repository.post.PostLikeRepository;
import com.codegym.socialmedia.repository.post.PostRepository;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import com.codegym.socialmedia.service.notification.PostMessage;
import com.codegym.socialmedia.service.notification.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PostServiceImpl implements PostService {
    @Autowired
    private IUserRepository userRepository;
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostMessage postMessage;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FriendshipService friendshipService;

    @Override
    public Post createPost(PostCreateDto dto, User user) {
        Post post = new Post();
        post.setUser(user);
        post.setContent(dto.getContent());
        post.setPrivacyLevel(dto.getPrivacyLevel());

        // Upload images if any
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : dto.getImages()) {
                if (!image.isEmpty()) {
                    String imageUrl = cloudinaryService.upload(image);
                    if (imageUrl != null) {
                        imageUrls.add(imageUrl);
                    }
                }
            }
            post.setImageUrls(convertListToJson(imageUrls));
        }

        return postRepository.save(post);
    }

    @Override
    public Post updatePost(Long postId, PostUpdateDto dto, User user) {
        Post post = postRepository.findByIdAndUser(postId, user)
                .orElseThrow(() -> new RuntimeException("Post not found or access denied"));

        post.setContent(dto.getContent());
        post.setPrivacyLevel(dto.getPrivacyLevel());
        post.setPrivacyCommentLevel(dto.getCommentPrivacyLevel());
        // Handle image updates (simplified)
        List<String> newImageUrls = new ArrayList<>();
        if (dto.getExistingImages() != null) {
            newImageUrls.addAll(dto.getExistingImages());
        }

        // Remove deleted images
        if (dto.getImagesToDelete() != null) {
            newImageUrls.removeAll(dto.getImagesToDelete());
        }

        // Add new images
        if (dto.getNewImages() != null && !dto.getNewImages().isEmpty()) {
            for (MultipartFile newImage : dto.getNewImages()) {
                if (!newImage.isEmpty()) {
                    String imageUrl = cloudinaryService.upload(newImage);
                    if (imageUrl != null) {
                        newImageUrls.add(imageUrl);
                    }
                }
            }
        }

        post.setImageUrls(convertListToJson(newImageUrls));
        return postRepository.save(post);
    }

    @Override
    public void deletePost(Long postId, User user) {
        Post post = postRepository.findByIdAndUser(postId, user)
                .orElseThrow(() -> new RuntimeException("Post not found or access denied"));

        post.setDeleted(true);
        postRepository.save(post);
    }

    @Override
    public PostDisplayDto getPostById(Long postId, User currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        return convertToDisplayDto(post, currentUser);
    }

    @Override
    public Page<PostDisplayDto> getPostsForNewsFeed(User currentUser, Pageable pageable) {

        Page<Post> posts = postRepository.findVisiblePosts(currentUser.getId(), pageable);

        return posts.map(post -> convertToDisplayDto(post, currentUser));
    }

    @Override
    public Page<PostDisplayDto> getPostsByUser(User targetUser, User currentUser, Pageable pageable) {
        Page<Post> posts;

        if (currentUser != null && currentUser.getId().equals(targetUser.getId())) {
            // Own posts - show all
            posts = postRepository.findByUserAndIsDeletedFalseOrderByCreatedAtDesc(targetUser, pageable);

        } else {
            // Others' posts (or friend) - show only public
            posts = postRepository.findVisiblePostsByUser(targetUser, currentUser, pageable);
        }

        return posts.map(post -> convertToDisplayDto(post, currentUser));
    }

    @Override
    public Page<PostDisplayDto> getPublicPostsByUser(User targetUser, User currentUser, Pageable pageable) {
        Page<Post> posts = postRepository.findVisiblePostsByUser(targetUser, currentUser, pageable);
        return posts.map(post -> convertToDisplayDto(post, null));
    }


    @Override
    public Page<PostDisplayDto> searchUserPosts(User user, User currentUser, String keyword, Pageable pageable) {
        Page<Post> posts = postRepository.searchPostsOnProfile(user, currentUser, keyword, pageable);
        return posts.map(post -> convertToDisplayDto(post, user));
    }

    @Override
    public boolean toggleLike(Long postId, User user) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        LikePostId likePostId = getLikeStatusId(postId, user.getId());
        boolean isLiked = postLikeRepository.findById(likePostId).isPresent();

        if (isLiked) {
            postLikeRepository.deleteByPostAndUser(post, user);
            postMessage.notifyLikeStatusChanged(postId, getLikeCount(post), false, user.getUsername());
            return false;
        } else {
            LikePost like = new LikePost();
            like.setPost(post);
            like.setUser(user);
            like.setId(likePostId);
            postLikeRepository.save(like);
            notificationService.notify(
                    user.getId(), post.getUser().getId(),
                    Notification.NotificationType.LIKE_POST,
                    Notification.ReferenceType.POST, postId);
            postMessage.notifyLikeStatusChanged(postId, getLikeCount(post), true, user.getUsername());
            return true;
        }
    }

    private static LikePostId getLikeStatusId(Long postId, Long userId) {
        LikePostId likeStatusId = new LikePostId();
        likeStatusId.setPostId(postId);
        likeStatusId.setUserId(userId);
        return likeStatusId;
    }

    @Override
    public List<User> getUsersWhoLiked(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        return postLikeRepository.findUsersWhoLikedPost(post);
    }


    @Override
    public long countUserPosts(User user) {
        return postRepository.countByUserAndIsDeletedFalse(user);
    }

    // Helper methods
    private PostDisplayDto convertToDisplayDto(Post post, User currentUser) {
        LikePostId likePostId = getLikeStatusId(post.getId(), currentUser.getId());
        boolean isLiked = currentUser != null &&
                postLikeRepository.findById(likePostId).isPresent();

        boolean canEdit = currentUser != null &&
                post.getUser().getId().equals(currentUser.getId());

        boolean canDelete = canEdit;

        PostDisplayDto dto = new PostDisplayDto(post, isLiked, canEdit, canDelete);

        if (currentUser.isAdmin()) {
            dto.setCanComment(true);
        }else{
            Friendship.FriendshipStatus friendshipStatus =
                friendshipService.getFriendshipStatus(post.getUser(), currentUser);
            boolean isFriend = (friendshipStatus == Friendship.FriendshipStatus.ACCEPTED);
            dto.setCanComment(PrivacyUtils.canView(currentUser, post.getUser(), post.getPrivacyCommentLevel(), isFriend));
        }

        dto.setLikesCount(getLikeCount(post));
        dto.setCommentsCount(countCommentsByPost(post));
        return dto;
    }

    @Override
    public int getLikeCount(Post post) {
        return postLikeRepository.countByPost(post);
    }

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Override
    public int countCommentsByPost(Post post) {
        return postCommentRepository.countByPost(post);
    }


    public List<String> getPhotosForProfile(User profileOwner, User viewer) {
        if (viewer == null) {
            return postRepository.findPublicPhotos(profileOwner);
        }
        return postRepository.findVisiblePhotos(profileOwner, viewer);
    }


    private String convertListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }


}