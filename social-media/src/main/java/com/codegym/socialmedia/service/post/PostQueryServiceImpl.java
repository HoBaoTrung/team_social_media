package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.component.privacy.PrivacyPolicyResolver;
import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Friendship;
import com.codegym.socialmedia.model.social_action.LikePostId;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.repository.FriendshipRepository;
import com.codegym.socialmedia.repository.post.PostCommentRepository;
import com.codegym.socialmedia.repository.post.PostLikeRepository;
import com.codegym.socialmedia.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryServiceImpl implements PostQueryService{

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostLikeRepository likeRepository;
    private final FriendshipRepository friendshipRepository;
    private final PrivacyPolicyResolver privacyPolicyResolver;

    public Page<PostDisplayDto> getPostsByUser(User target, User viewer, Pageable pageable) {
        Page<Post> posts = postRepository.findVisiblePostsByUser(target, viewer, pageable);
        List<PostDisplayDto> dtos = convertPostsToDto(posts.getContent(), viewer);
        return new PageImpl<>(dtos, pageable, posts.getTotalElements());
    }

    @Override
    public Page<PostDisplayDto> getPublicPostsByUser(User targetUser, User currentUser, Pageable pageable) {
        Page<Post> posts = postRepository.findVisiblePostsByUser(targetUser, currentUser, pageable);
        List<PostDisplayDto> dtos = convertPostsToDto(posts.getContent(), currentUser);
        return new PageImpl<>(dtos, pageable, posts.getTotalElements());
    }

    @Override
    public Page<PostDisplayDto> searchUserPosts(User user, User currentUser, String keyword, Pageable pageable) {
        Page<Post> posts = postRepository.searchPostsOnProfile(user, currentUser, keyword, pageable);
        List<PostDisplayDto> dtos = convertPostsToDto(posts.getContent(), currentUser);
        return new PageImpl<>(dtos, pageable, posts.getTotalElements());
    }

    public PostDisplayDto getPostById(Long postId, User viewer) {
        Post post = postRepository.findById(postId).orElseThrow();
        return toDto(post, viewer, null, null, null);
    }

    // Optimized batch method to avoid N+1 queries (Fix N+1)
    private List<PostDisplayDto> convertPostsToDto(List<Post> posts, User viewer) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());

        // Batch query: Get all likes count
        Map<Long, Integer> likeCountMap = likeRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        PostLikeRepository.LikeCountProjection::getPostId,
                        proj -> proj.getLikeCount().intValue()
                ));

        // Batch query: Get all comments count
        Map<Long, Integer> commentCountMap = commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        PostCommentRepository.CommentCountProjection::getPostId,
                        proj -> proj.getCommentCount().intValue()
                ));

        // Batch query: Get liked post IDs by user
        Set<Long> likedPostIds = viewer != null ?
                likeRepository.findLikedPostIdsByUser(viewer.getId(), postIds) :
                Collections.emptySet();

        // Batch query: Get all friend statuses at once
        Set<Long> ownerIds = posts.stream()
                .map(Post::getUser)
                .map(User::getId)
                .collect(Collectors.toSet());

        Map<Long, Friendship.FriendshipStatus> friendshipMap =
                friendshipRepository.findFriendIdsWithStatus(viewer.getId())
                        .stream()
                        .collect(Collectors.toMap(
                                f -> f.getFriendId(viewer.getId()),
                                FriendshipRepository.FriendIdWithStatus::getStatus,
                                (oldValue, newValue) -> oldValue
                        ));



        return posts.stream()
                .map(post -> toDto(post, viewer, likeCountMap, commentCountMap, friendshipMap))
                .collect(Collectors.toList());
    }

    private PostDisplayDto toDto(Post post, User viewer,
                                 Map<Long, Integer> likeCountMap,
                                 Map<Long, Integer> commentCountMap,
                                 Map<Long, Friendship.FriendshipStatus> friendshipMap) {

        boolean liked = viewer != null && (likeCountMap != null ? false :
                likeRepository.existsById(new LikePostId(post.getId(), viewer.getId())));

        boolean canEdit = viewer != null && post.getUser().getId().equals(viewer.getId());

        // If we have pre-fetched data, use it; otherwise fall back to single queries
        int likeCount = likeCountMap != null ?
                likeCountMap.getOrDefault(post.getId(), 0) :
                0;
        int commentCount = commentCountMap != null ?
                commentCountMap.getOrDefault(post.getId(), 0) :
                0;

        PostDisplayDto dto = new PostDisplayDto(post, liked, canEdit, canEdit);
        dto.setLikesCount(likeCount);
        dto.setCommentsCount(commentCount);

        if (viewer != null) {
            Friendship.FriendshipStatus status = friendshipMap != null ?
                    friendshipMap.getOrDefault(post.getUser().getId(), Friendship.FriendshipStatus.NONE) :
                    (friendshipRepository.findFriendshipBetween(post.getUser(), viewer)
                            .map(Friendship::getStatus)
                            .orElse(Friendship.FriendshipStatus.NONE));

            boolean isFriend = status == Friendship.FriendshipStatus.ACCEPTED;
            dto.setCanComment(
                    privacyPolicyResolver.canView(viewer.getId(), post.getUser().getId(),
                            post.getPrivacyCommentLevel(), isFriend)
            );
        }

        return dto;
    }
}
