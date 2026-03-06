package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.dto.post.FeedResponse;
import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Friendship;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.repository.FriendshipRepository;
import com.codegym.socialmedia.repository.post.PostRepository;
import com.codegym.socialmedia.service.RedisFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostFeedServiceImpl implements PostFeedService {

    private final RedisFeedService redisFeedService;
    private final PostRepository postRepository;
    private final FriendshipRepository friendshipRepository;
    private final PostMapper postMapper;

    @Override
    public FeedResponse getFeed(
            User currentUser,
            Long lastScore,
            int size
    ) {

        Long viewerId = currentUser.getId();

        if (lastScore == null) {
            lastScore = Long.MAX_VALUE;
        }


        List<Long> mergedIds = redisFeedService.getMergedFeedIds(viewerId, lastScore, size);

        if (mergedIds.isEmpty()) {
            return new FeedResponse(List.of(), null);
        }

        // Lấy friendIds để filter privacy
        List<Long> friendIds = friendshipRepository
                .findAllFriendshipsOfUser(viewerId)
                .stream()
                .map(f -> f.getRequester().getId().equals(viewerId)
                        ? f.getAddressee().getId()
                        : f.getRequester().getId())
                .toList();

        if (friendIds.isEmpty()) {
            friendIds = List.of(-1L);
        }

        // Filter privacy tại DB
        List<Post> visiblePosts =
                postRepository.findVisiblePostsByIds(
                        viewerId,
                        mergedIds,
                        friendIds
                );

        // Sort đúng thứ tự theo score Redis
        Map<Long, Post> postMap = visiblePosts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<Post> sorted = mergedIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .limit(size)
                .toList();

        // Map DTO
        List<PostDisplayDto> dtos = sorted.stream()
                .map(p -> postMapper.toDisplayDto(p, currentUser))
                .toList();

        // Next cursor
        Long nextCursor = sorted.isEmpty()
                ? null
                : sorted.get(sorted.size() - 1)
                .getCreatedAt()
                .toEpochSecond(ZoneOffset.UTC);

        return new FeedResponse(dtos, nextCursor);
    }
}