package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.model.account.User;

import java.util.List;

public interface PostStatService {
    long countUserPosts(User user);
    List<String> getPhotosForProfile(User profileOwner, User viewer);
}
