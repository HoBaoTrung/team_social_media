package com.codegym.socialmedia.service.user;

import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import com.codegym.socialmedia.service.post.PostStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserStatsService {

    @Autowired
    private FriendshipService friendshipService;

    @Autowired private PostStatService postStatService;

    public Map<String, Long> getUserStats(User user) {
        Map<String, Long> stats = new HashMap<>();

        try {
            // Count friends
            long friendsCount = friendshipService.countFriends(user.getId());
            stats.put("friends", friendsCount);
        } catch (Exception e) {
            stats.put("friends", 0L);
        }

        try {
            // Count posts
            long postsCount = postStatService.countUserPosts(user);
            stats.put("posts", postsCount);
        } catch (Exception e) {
            stats.put("posts", 0L);
        }

        // TẠM THỜI hardcode likes = 0 (sẽ implement sau)
        stats.put("likes", 0L);

        return stats;
    }
}