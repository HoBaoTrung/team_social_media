package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.component.privacy.PrivacyPolicyResolver;
import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Friendship;
import com.codegym.socialmedia.model.social_action.LikePostId;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.repository.post.PostCommentRepository;
import com.codegym.socialmedia.repository.post.PostLikeRepository;
import com.codegym.socialmedia.repository.post.PostRepository;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryServiceImpl implements PostQueryService{

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final FriendshipService friendshipService;
    private final PrivacyPolicyResolver privacyPolicyResolver;
    public Page<PostDisplayDto> getPostsByUser(User target, User viewer, Pageable pageable) {
        Page<Post> posts = postRepository.findVisiblePostsByUser(target, viewer, pageable);
        return posts.map(p -> toDto(p, viewer));
    }

    @Override
    public Page<PostDisplayDto> getPublicPostsByUser(User targetUser, User currentUser, Pageable pageable) {
        Page<Post> posts = postRepository.findVisiblePostsByUser(targetUser, currentUser, pageable);
        return posts.map(post -> toDto(post, currentUser));
    }

    @Override
    public Page<PostDisplayDto> searchUserPosts(User user, User currentUser, String keyword, Pageable pageable) {
        Page<Post> posts = postRepository.searchPostsOnProfile(user, currentUser, keyword, pageable);
        return posts.map(post -> toDto(post, currentUser));
    }

    public PostDisplayDto getPostById(Long postId, User viewer) {
        Post post = postRepository.findById(postId).orElseThrow();
        return toDto(post, viewer);
    }

//    @Override
//    public Page<PostDisplayDto> getPostsForNewsFeed(User currentUser, Pageable pageable) {
//        return null;
//    }

    private PostDisplayDto toDto(Post post, User viewer) {
        boolean liked = viewer != null &&
                likeRepository.existsById(new LikePostId(post.getId(), viewer.getId()));

        boolean canEdit = viewer != null &&
                post.getUser().getId().equals(viewer.getId());

        PostDisplayDto dto = new PostDisplayDto(post, liked, canEdit, canEdit);

        if (viewer != null) {
            boolean isFriend = friendshipService.getFriendshipStatus(post.getUser(), viewer).equals(Friendship.FriendshipStatus.ACCEPTED);
            dto.setCanComment(
                    privacyPolicyResolver.canView(viewer, post.getUser(),
                            post.getPrivacyCommentLevel(), isFriend)
            );
        }

        dto.setLikesCount(likeRepository.countByPost(post));
        dto.setCommentsCount(commentRepository.countByPost(post));
        return dto;
    }
}
