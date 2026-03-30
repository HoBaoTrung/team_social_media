
package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.component.privacy.PrivacyPolicyResolver;
import com.codegym.socialmedia.dto.post.FeedResponse;
import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Friendship;
import com.codegym.socialmedia.model.social_action.Post;
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
    private final FriendshipService friendshipService;
    private final PrivacyPolicyResolver privacyPolicyResolver;
    private final PostMapper postMapper;
    private final FriendshipRepository friendshipRepository;
    @Override
    public FeedResponse getFeed(User currentUser, Long lastScore, int size) {

        Long viewerId = currentUser.getId();
        if (lastScore == null) {
            lastScore = Long.MAX_VALUE;
        }

        List<Long> mergedIds = redisFeedService.getMergedFeedIds(viewerId, lastScore, size);
        mergedIds = new ArrayList<>(new LinkedHashSet<>(mergedIds));

        if (mergedIds.isEmpty()) {
            return new FeedResponse(List.of(), null);
        }

        final List<Long> friendIds = friendshipRepository.findAllFriendshipsOfUser(viewerId)
                .stream()
                .map(f -> f.getRequester().getId().equals(viewerId)
                        ? f.getAddressee().getId()
                        : f.getRequester().getId())
                .toList();
        Set<Long> friendIdSet = new HashSet<>(friendIds);
        Map<Long, Friendship.FriendshipStatus> friendshipMap = friendIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> Friendship.FriendshipStatus.ACCEPTED
                ));
//        List<Post> visiblePosts = postRepository.findVisiblePostsByIds(viewerId, mergedIds, friendIds);
        List<Post> visiblePosts = postRepository.findByIdIn(mergedIds);

        visiblePosts = visiblePosts.stream()
                .filter(post -> privacyPolicyResolver.canView(
                        currentUser,
                        post,
                        post.getPrivacyLevel(),
                        friendIdSet.contains(post.getOwnerId())
                        ))
                .collect(Collectors.toList());


        Map<Long, Post> postMap = visiblePosts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<Post> sortedPosts = new ArrayList<>();
        List<Long> postOwnerIds = new ArrayList<>();

        mergedIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .limit(size)
                .forEach(post -> {
                    sortedPosts.add(post);
                    postOwnerIds.add(post.getOwnerId());
                });

        Set<Long> likedPostIds = postLikeRepository.findLikedPostIdsByUser(viewerId, mergedIds);

        Map<Long, Integer> likeCountMap = postLikeRepository.countByPostIds(mergedIds)
                .stream()
                .collect(Collectors.toMap(
                        PostLikeRepository.LikeCountProjection::getPostId,
                        proj -> proj.getLikeCount().intValue()
                ));

        Map<Long, Integer> commentCountMap = postCommentRepository.countByPostIds(mergedIds)
                .stream()
                .collect(Collectors.toMap(
                        PostCommentRepository.CommentCountProjection::getPostId,
                        proj -> proj.getCommentCount().intValue()
                ));

//        Map<Long, Friendship.FriendshipStatus> friendshipMap =
//                friendshipService.getFriendshipMap(currentUser, postOwnerIds);

        List<PostDisplayDto> dtos = sortedPosts.stream()
                .map(post -> {
                    Long postId = post.getId();
                    Long ownerId = post.getUser().getId();

                    boolean isLiked = likedPostIds.contains(postId);
                    int likeCount = likeCountMap.getOrDefault(postId, 0);
                    int commentCount = commentCountMap.getOrDefault(postId, 0);

                    boolean isFriend = friendshipMap.getOrDefault(ownerId, Friendship.FriendshipStatus.NONE)
                            == Friendship.FriendshipStatus.ACCEPTED;

                    boolean canEdit = ownerId.equals(viewerId);
                    boolean canDelete = canEdit;

                    boolean canComment = currentUser.isAdmin() ||
                            privacyPolicyResolver.canView(currentUser, post.getUser(),
                                    post.getPrivacyCommentLevel(), isFriend);

                    return postMapper.toDisplayDto(post, isLiked, likeCount, commentCount,
                            canComment, canEdit, canDelete);
                })
                .toList();


        // Next cursor
        Long nextCursor = sortedPosts.isEmpty() ? null :
                sortedPosts.get(sortedPosts.size() - 1)
                        .getCreatedAt()
                        .toEpochSecond(ZoneOffset.UTC);

        return new FeedResponse(dtos, nextCursor);
    }
}