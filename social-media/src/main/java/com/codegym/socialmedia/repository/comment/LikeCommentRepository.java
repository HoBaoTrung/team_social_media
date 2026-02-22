package com.codegym.socialmedia.repository.comment;

import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.LikeComment;
import com.codegym.socialmedia.model.social_action.LikeCommentId;
import com.codegym.socialmedia.model.social_action.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface LikeCommentRepository extends JpaRepository<LikeComment, LikeCommentId> {

    int countByComment(PostComment comment);

    boolean existsByCommentAndUser(PostComment comment,User commentUser);

    @Modifying
    @Query("delete from LikeComment l where l.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);
}


