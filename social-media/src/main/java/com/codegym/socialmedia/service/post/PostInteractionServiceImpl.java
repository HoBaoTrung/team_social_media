package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.LikePost;
import com.codegym.socialmedia.model.social_action.LikePostId;
import com.codegym.socialmedia.model.social_action.Notification;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.repository.post.PostLikeRepository;
import com.codegym.socialmedia.repository.post.PostRepository;
import com.codegym.socialmedia.service.notification.NotificationService;
import com.codegym.socialmedia.service.notification.PostMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostInteractionServiceImpl implements PostInteractionService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final NotificationService notificationService;
    private final PostMessage postMessage;

    public boolean toggleLike(Long postId, User user) {
        Post post = postRepository.findById(postId).orElseThrow();

        LikePostId id = new LikePostId(user.getId(), postId);
        boolean liked = postLikeRepository.existsById(id);

        if (liked) {
            postLikeRepository.deleteByPostAndUser(post, user);
            postMessage.notifyLikeStatusChanged(
                    postId, postLikeRepository.countByPost(post), false, user.getUsername());
            return false;
        }

        LikePost like = new LikePost();
        like.setId(id);
        like.setPost(post);
        like.setUser(user);
        postLikeRepository.save(like);

        notificationService.notify(
                user.getId(), post.getUser().getId(),
                Notification.NotificationType.LIKE_POST,

                postId
        );

        postMessage.notifyLikeStatusChanged(
                postId, postLikeRepository.countByPost(post), true, user.getUsername());

        return true;
    }

    @Override
    public List<User> getUsersWhoLiked(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        return postLikeRepository.findUsersWhoLikedPost(post);
    }
}
