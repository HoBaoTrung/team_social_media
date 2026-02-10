package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.model.account.User;

import java.util.List;

public interface PostInteractionService {
    boolean toggleLike(Long postId, User user);

    List<User> getUsersWhoLiked(Long postId);
}
