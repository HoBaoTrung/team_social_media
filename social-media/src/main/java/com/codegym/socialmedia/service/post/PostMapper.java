package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.repository.post.PostRepository;
import org.springframework.stereotype.Component;
@Component
public class PostMapper {

    public PostDisplayDto toDisplayDto(
            PostRepository.PostFeedProjection post,
            boolean isLiked,
            int likeCount,
            int commentCount,
            boolean canComment,
            boolean canEdit,
            boolean canDelete
    ) {

        PostDisplayDto dto = new PostDisplayDto(
                post,
                isLiked,
                canEdit,
                canDelete
        );

        dto.setLikesCount(likeCount);
        dto.setCommentsCount(commentCount);
        dto.setCanComment(canComment);

        return dto;
    }
}