package com.codegym.socialmedia.service.redis;

import com.codegym.socialmedia.repository.FriendshipRepository;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisFriend {
    private final StringRedisTemplate redisTemplate;
    private final FriendshipService friendshipService;
    public Set<Long> friends(long userId) {
        String key = "user:" + userId + ":friends";

        // 1. Lấy từ Redis
        Set<String> cached = redisTemplate.opsForSet().members(key);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toSet());
        }

        // 2. Không có → load từ DB
        Set<Long> friendsFromDb = friendshipService.findFriendIdsOfUser(userId);

        if (friendsFromDb.isEmpty()) {
            redisTemplate.opsForValue().set(key, "EMPTY", Duration.ofMinutes(5));
        }

        // 3. Cache vào Redis (nếu có data)
        if (friendsFromDb != null && !friendsFromDb.isEmpty()) {
            Set<String> values = friendsFromDb.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toSet());

            redisTemplate.opsForSet().add(key, values.toArray(new String[0]));

            // TTL (rất quan trọng)
            redisTemplate.expire(key, Duration.ofMinutes(10));
        }

        return friendsFromDb;
    }
}
