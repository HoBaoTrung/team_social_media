// PostService.java
package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.dto.post.PostCreateDto;
import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.dto.post.PostUpdateDto;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostService {

    // CRUD Operations
    Post createPost(PostCreateDto dto, User user);
    Post updatePost(Long postId, PostUpdateDto dto, User user);
    void deletePost(Long postId, User user);
    PostDisplayDto getPostById(Long postId, User currentUser);

    // Get posts for different contexts
    Page<PostDisplayDto> getPostsForNewsFeed(User currentUser, Pageable pageable);
    Page<PostDisplayDto> getPostsByUser(User targetUser, User currentUser, Pageable pageable);
    Page<PostDisplayDto> getPublicPostsByUser(User targetUser, User currentUser ,Pageable pageable);
    Page<PostDisplayDto> searchUserPosts(User user,User currentUser, String keyword, Pageable pageable);

    // Like functionality
    boolean toggleLike(Long postId, User user);
    List<User> getUsersWhoLiked(Long postId);
    int getLikeCount(Post id);
    int countCommentsByPost(Post post);
    List<String> getPhotosForProfile (User profileOwner, User viewer);
    // Utility
    long countUserPosts(User user);
}