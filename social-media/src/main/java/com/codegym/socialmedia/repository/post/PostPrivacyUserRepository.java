package com.codegym.socialmedia.repository.post;


import com.codegym.socialmedia.model.social_action.PostPrivacyUser;
import com.codegym.socialmedia.model.social_action.PostPrivacyUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostPrivacyUserRepository extends JpaRepository<PostPrivacyUser, PostPrivacyUserId> {
    @Modifying
    @Query("delete from PostPrivacyUser p where p.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Query("""
    select p.user.id
    from PostPrivacyUser p
    where p.post.id = :postId
      and p.accept = true
""")
    List<Long> findAllowedUserIds(Long postId);

    @Query("""
    select p.user.id
    from PostPrivacyUser p
    where p.post.id = :postId
      and p.accept = false
""")
    List<Long> findExcludedUserIds(Long postId);
}
