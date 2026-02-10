package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.model.account.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostQueryService {
    PostDisplayDto getPostById(Long postId, User currentUser);

//    Page<PostDisplayDto> getPostsForNewsFeed(User currentUser, Pageable pageable);

    Page<PostDisplayDto> getPostsByUser(User targetUser, User currentUser, Pageable pageable);

    Page<PostDisplayDto> getPublicPostsByUser(User targetUser, User currentUser, Pageable pageable);

    Page<PostDisplayDto> searchUserPosts(
            User user,
            User currentUser,
            String keyword,
            Pageable pageable
    );
}
