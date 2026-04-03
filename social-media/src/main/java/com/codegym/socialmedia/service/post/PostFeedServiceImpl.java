
package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.component.privacy.PrivacyPolicyResolver;
import com.codegym.socialmedia.dto.post.FeedResponse;
import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.model.account.AuthUser;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Friendship;
import com.codegym.socialmedia.model.PrivacyLevel;
import com.codegym.socialmedia.repository.FriendshipRepository;
import com.codegym.socialmedia.repository.post.PostCommentRepository;
import com.codegym.socialmedia.repository.post.PostLikeRepository;
import com.codegym.socialmedia.repository.post.PostRepository;
import com.codegym.socialmedia.service.RedisFeedService;
import com.codegym.socialmedia.service.friend_ship.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostFeedServiceImpl implements PostFeedService {

    private final PostLikeRepository postLikeRepository;
    private final RedisFeedService redisFeedService;
    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final PrivacyPolicyResolver privacyPolicyResolver;
    private final PostMapper postMapper;
    private final FriendshipRepository friendshipRepository;

    @Override
    public FeedResponse getFeed(AuthUser currentUser, Long lastScore, int size) {

        Long viewerId = currentUser.getId();
        if (lastScore == null) {
            lastScore = Long.MAX_VALUE;
        }

        List<Long> mergedIds = redisFeedService.getMergedFeedIds(viewerId, lastScore, size);
        mergedIds = new ArrayList<>(new LinkedHashSet<>(mergedIds));

        if (mergedIds.isEmpty()) {
            return new FeedResponse(List.of(), null);
        }

        List<PostRepository.PostFeedProjection> visiblePosts = postRepository.findFeedPosts(mergedIds);

        Set<Long> ownerIdsToCheck = visiblePosts.stream()
                .map(PostRepository.PostFeedProjection::getOwnerId)
                .filter(ownerId -> !ownerId.equals(viewerId))   // không cần check bản thân
                .collect(Collectors.toSet());

        List<Long> ownerIdsList = new ArrayList<>(ownerIdsToCheck);

        Map<Long, Friendship.FriendshipStatus> friendshipMap = friendshipRepository
                .findFriendshipStatusBatch(viewerId, ownerIdsList)
                .stream()
                .collect(Collectors.toMap(
                        f -> {
                            return f.getFriendId(viewerId);
                        },
                        FriendshipRepository.FriendIdWithStatus::getStatus,
                        (oldValue, newValue) -> oldValue
                ));

        List<PostRepository.PostFeedProjection> filteredPosts = visiblePosts.stream()
                .filter(projection -> {

                    Long ownerId = projection.getOwnerId();

                    // Luôn cho xem bài của chính mình
                    if (ownerId.equals(viewerId)) {
                        return true;
                    }

                    // Admin xem tất cả
                    if (currentUser.isAdmin()) {
                        return true;
                    }

                    boolean isView = privacyPolicyResolver.canView(
                            viewerId,
                            ownerId,
                            projection.getPrivacyLevel(),
                            friendshipMap.getOrDefault(ownerId, Friendship.FriendshipStatus.NONE)
                                    == Friendship.FriendshipStatus.ACCEPTED
                    );
                    return  isView;

                })
                .collect(Collectors.toList());


        Map<Long, PostRepository.PostFeedProjection> postMap = filteredPosts.stream()
                .collect(Collectors.toMap(PostRepository.PostFeedProjection::getId, Function.identity()));

        List<PostRepository.PostFeedProjection> sortedPosts = mergedIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .limit(size)
                .toList();

        List<Long> sortedPostIds = sortedPosts.stream()
                .map(PostRepository.PostFeedProjection::getId)
                .collect(Collectors.toList());

        // Step 4: Batch fetch likes and comments
        Set<Long> likedPostIds = postLikeRepository.findLikedPostIdsByUser(viewerId, sortedPostIds);

        Map<Long, Integer> likeCountMap = postLikeRepository.countByPostIds(sortedPostIds)
                .stream()
                .collect(Collectors.toMap(
                        PostLikeRepository.LikeCountProjection::getPostId,
                        proj -> proj.getLikeCount().intValue()
                ));

        Map<Long, Integer> commentCountMap = postCommentRepository.countByPostIds(sortedPostIds)
                .stream()
                .collect(Collectors.toMap(
                        PostCommentRepository.CommentCountProjection::getPostId,
                        proj -> proj.getCommentCount().intValue()
                ));

        List<PostDisplayDto> dtos = sortedPosts.stream()
                .map(postFeedProjection -> {
                    Long postId = postFeedProjection.getId();
                    Long ownerId = postFeedProjection.getOwnerId();

                    boolean isLiked = likedPostIds.contains(postId);
                    int likeCount = likeCountMap.getOrDefault(postId, 0);
                    int commentCount = commentCountMap.getOrDefault(postId, 0);

                    boolean isFriend = friendshipMap.getOrDefault(ownerId, Friendship.FriendshipStatus.NONE)
                            == Friendship.FriendshipStatus.ACCEPTED;

                    boolean canEdit = ownerId.equals(viewerId);
                    boolean canDelete = canEdit;

                    boolean canComment = currentUser.isAdmin() ||
                            privacyPolicyResolver.canView(
                                    viewerId,
                                    ownerId,
                                    postFeedProjection.getPrivacyCommentLevel(),
                                    isFriend
                            );

                    return postMapper.toDisplayDto(postFeedProjection, isLiked, likeCount, commentCount,
                            canComment, canEdit, canDelete);
                })
                .toList();

        Long nextCursor = sortedPosts.isEmpty() ? null :
                sortedPosts.get(sortedPosts.size() - 1)
                        .getCreatedAt()
                        .toEpochSecond(ZoneOffset.UTC);

        return new FeedResponse(dtos, nextCursor);
    }
}