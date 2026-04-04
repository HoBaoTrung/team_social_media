package com.codegym.socialmedia.service.redis;

import com.codegym.socialmedia.model.PrivacyLevel;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.repository.FriendshipRepository;
import com.codegym.socialmedia.repository.post.PostRepository;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisFeedService {

    private static final String PUBLIC_FEED_KEY = "feed:public";
    private static final String USER_FEED_KEY = "feed:%d";
    private static final String AUTHOR_FEED_KEY = "posts:author:%d";

    private static final int MAX_FEED_SIZE = 10000;
    private static final int CELEBRITY_THRESHOLD = 1000;

    private final FriendshipRepository friendshipRepository;
    private final StringRedisTemplate redis;
    private final PostRepository postRepository;
    private final RedisFriend redisFriend;
    private String userKey(Long userId) {
        return String.format(USER_FEED_KEY, userId);
    }

    private String authorKey(Long authorId) {
        return String.format(AUTHOR_FEED_KEY, authorId);
    }

    public List<Long> getMergedFeedIds(Long userId, Long maxScore, int size) {

        String userFeedKey = userKey(userId);

        if (Boolean.FALSE.equals(redis.hasKey(userFeedKey))) {
            warmUpUserFeed(userId);
        }

        if (Boolean.FALSE.equals(redis.hasKey(PUBLIC_FEED_KEY))) {
            warmUpPublicFeed();
        }

        int buffer = size * 3;

        Set<ZSetOperations.TypedTuple<String>> userFeed =
                redis.opsForZSet()
                        .reverseRangeByScoreWithScores(
                                userFeedKey,
                                0,
                                maxScore - 1,
                                0,
                                buffer
                        );

        Set<ZSetOperations.TypedTuple<String>> publicFeed =
                redis.opsForZSet()
                        .reverseRangeByScoreWithScores(
                                PUBLIC_FEED_KEY,
                                0,
                                maxScore - 1,
                                0,
                                buffer
                        );

        List<Long> celebrityIds = getCelebrityFriends(userId);

        List<ZSetOperations.TypedTuple<String>> celebrityPosts = new ArrayList<>();

        for (Long celebId : celebrityIds) {

            Set<ZSetOperations.TypedTuple<String>> posts =
                    redis.opsForZSet()
                            .reverseRangeByScoreWithScores(
                                    authorKey(celebId),
                                    0,
                                    maxScore - 1,
                                    0,
                                    5
                            );

            if (posts != null) {
                celebrityPosts.addAll(posts);
            }
        }

        List<ZSetOperations.TypedTuple<String>> merged = new ArrayList<>();

        if (userFeed != null) merged.addAll(userFeed);
        if (publicFeed != null) merged.addAll(publicFeed);
        merged.addAll(celebrityPosts);

        return merged.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(size * 2)
                .map(t -> Long.valueOf(t.getValue()))
                .toList();
    }


    public void fanOutPost(Post post) {

        Long ownerId = post.getUser().getId();
        Long postId = post.getId();

        long score = post.getCreatedAt().toEpochSecond(ZoneOffset.UTC);

        long followerCount = friendshipRepository.countFriendsByUserId(ownerId);

        boolean isCelebrity = followerCount >= CELEBRITY_THRESHOLD;

        redis.opsForZSet().add(
                authorKey(ownerId),
                postId.toString(),
                score
        );

        if (!isCelebrity) {

            Set<Long> friendIds =
                    redisFriend.friends(ownerId);

            for (Long friendId : friendIds) {

                redis.opsForZSet().add(
                        userKey(friendId),
                        postId.toString(),
                        score
                );

                trim(userKey(friendId));
            }
        }

        redis.opsForZSet().add(
                userKey(ownerId),
                postId.toString(),
                score
        );

        if (post.getPrivacyLevel() == PrivacyLevel.PUBLIC) {

            redis.opsForZSet().add(
                    PUBLIC_FEED_KEY,
                    postId.toString(),
                    score
            );

            trim(PUBLIC_FEED_KEY);
        }

        trim(authorKey(ownerId));
    }


    private List<Long> getCelebrityFriends(Long userId) {
        return friendshipRepository.findCelebrityFriendIds(userId, CELEBRITY_THRESHOLD);
    }

    private void warmUpUserFeed(Long viewerId) {

        Set<Long> friendIds =
                redisFriend.friends(viewerId);

        Pageable pageable = PageRequest.of(0, 100);

        List<Post> candidates =
                postRepository.findWarmUpCandidates(
                        viewerId,
                        friendIds.stream().toList(),
                        pageable
                );

        for (Post post : candidates) {

            redis.opsForZSet().add(
                    userKey(viewerId),
                    post.getId().toString(),
                    post.getCreatedAt().toEpochSecond(ZoneOffset.UTC)
            );
        }
    }

    private void warmUpPublicFeed() {

        List<Post> posts =
                postRepository
                        .findTop100ByPrivacyLevelOrderByCreatedAtDesc(
                                PrivacyLevel.PUBLIC
                        );

        for (Post post : posts) {

            redis.opsForZSet().add(
                    PUBLIC_FEED_KEY,
                    post.getId().toString(),
                    post.getCreatedAt().toEpochSecond(ZoneOffset.UTC)
            );
        }
    }

    private void trim(String key) {
        redis.opsForZSet().removeRange(key, 0, -MAX_FEED_SIZE - 1);
    }
}