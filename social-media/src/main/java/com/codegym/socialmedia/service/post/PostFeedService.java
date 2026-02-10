package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.model.account.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostFeedService {
    Page<PostDisplayDto> getFeed(User currentUser, Pageable pageable);

}
