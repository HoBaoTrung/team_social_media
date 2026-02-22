package com.codegym.socialmedia.service.post;

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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostFeedServiceImpl implements  PostFeedService {

    private final RedisFeedService redisFeedService;
    private final PostRepository postRepository;
    private final FriendshipRepository friendshipRepository;
    private final PostMapper postMapper;

    @Override
    public Page<PostDisplayDto> getFeed(User currentUser, Pageable pageable) {

        List<Long> postIds = redisFeedService.getFeedIds(
                currentUser.getId(),
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        if (postIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Post> posts = postRepository.findByIdIn(postIds);

        Map<Long, Post> postMap = posts.stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        List<PostDisplayDto> dtos = postIds.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .map(p -> postMapper.toDisplayDto(p, currentUser))
                .toList();

        return new PageImpl<>(
                dtos,
                pageable,
                redisFeedService.getTotalFeedCount(currentUser.getId())
        );
    }

}
