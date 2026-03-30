package com.codegym.socialmedia.service.post;

import com.codegym.socialmedia.dto.post.PostDisplayDto;
import com.codegym.socialmedia.model.social_action.Post;
import org.springframework.stereotype.Component;
@Component
public class PostMapper {

    public PostDisplayDto toDisplayDto(
            Post post,
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