package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.dto.post.PostCreateDto;
import com.codegym.socialmedia.dto.post.PostUpdateDto;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Post;

public interface PostCommandService {

    Post createPost(PostCreateDto dto, User user);

    Post updatePost(Long postId, PostUpdateDto dto, User user);

    void deletePost(Long postId, User user);
}