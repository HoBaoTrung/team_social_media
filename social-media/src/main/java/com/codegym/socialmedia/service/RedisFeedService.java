package com.codegym.socialmedia.service;

import com.codegym.socialmedia.model.social_action.Post;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

@Service
public class RedisFeedService {

    private static final String FEED_KEY = "feed:%d";
    private final StringRedisTemplate redis;

    public RedisFeedService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void pushToFeed(Long userId, Long postId, long createdAt) {
        String key = String.format(FEED_KEY, userId);
        redis.opsForZSet().add(key, postId.toString(), createdAt);
    }

    public List<Long> getFeed(Long userId, int page, int size) {
        String key = String.format(FEED_KEY, userId);
        int start = page * size;
        int end = start + size - 1;

        Set<String> ids = redis.opsForZSet()
                .reverseRange(key, start, end);

        if (ids == null) return List.of();

        return ids.stream().map(Long::valueOf).toList();
    }

    public void trimFeed(Long userId, int maxSize) {
        String key = String.format(FEED_KEY, userId);
        redis.opsForZSet().removeRange(key, 0, -maxSize - 1);
    }

    public void warmUpFeed(Long userId, List<Post> posts) {
        for (Post post : posts) {
            pushToFeed(
                    userId,
                    post.getId(),
                    post.getCreatedAt().toEpochSecond(ZoneOffset.UTC)
            );
        }
    }

    public long getTotalFeedCount(Long userId){
        String key = String.format(FEED_KEY, userId);

        Long count = redis.opsForZSet().size(key);

        return count != null ? count : 0L;
    }
}
