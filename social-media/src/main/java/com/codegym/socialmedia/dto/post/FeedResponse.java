package com.codegym.socialmedia.dto.post;

import com.codegym.socialmedia.model.account.User;

import java.util.List;

public record FeedResponse(List<PostDisplayDto> posts, Long nextCursor) { }
