package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.component.privacy.PrivacyPolicyResolver;
import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Friendship;
import com.codegym.socialmedia.model.social_action.LikePostId;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.repository.post.PostCommentRepository;
import com.codegym.socialmedia.repository.post.PostLikeRepository;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import com.codegym.socialmedia.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired private PrivacyPolicyResolver privacyPolicyResolver;

    private LikePostId getLikeStatusId(Long postId, Long userId) {
        LikePostId likeStatusId = new LikePostId();
        likeStatusId.setPostId(postId);
        likeStatusId.setUserId(userId);
        return likeStatusId;
    }
    public PostDisplayDto toDisplayDto(Post post, User currentUser) {
        LikePostId likePostId = getLikeStatusId(post.getId(), currentUser.getId());
        boolean isLiked = currentUser != null &&
                postLikeRepository.findById(likePostId).isPresent();

        boolean canEdit = currentUser != null &&
                post.getUser().getId().equals(currentUser.getId());

        boolean canDelete = canEdit;

        PostDisplayDto dto = new PostDisplayDto(post, isLiked, canEdit, canDelete);

        if (currentUser.isAdmin()) {
            dto.setCanComment(true);
        }else{
            Friendship.FriendshipStatus friendshipStatus =
                    friendshipService.getFriendshipStatus(post.getUser(), currentUser);
            boolean isFriend = (friendshipStatus == Friendship.FriendshipStatus.ACCEPTED);
            dto.setCanComment(privacyPolicyResolver.canView(currentUser, post.getUser(), post.getPrivacyCommentLevel(), isFriend));
        }

        dto.setLikesCount(getLikeCount(post));
        dto.setCommentsCount(countCommentsByPost(post));
        return dto;
    }

    private int getLikeCount(Post post) {
        return postLikeRepository.countByPost(post);
    }

    private int countCommentsByPost(Post post) {
        return postCommentRepository.countByPost(post);
    }
}
