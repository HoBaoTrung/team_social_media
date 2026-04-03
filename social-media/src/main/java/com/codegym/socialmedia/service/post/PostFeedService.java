package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.dto.post.FeedResponse;
import com.codegym.socialmedia.model.account.AuthUser;

public interface PostFeedService {
    FeedResponse getFeed(AuthUser currentUser, Long lastScore, int size);

}
