// PostLikeRepository.java
package com.codegym.socialmedia.repository.post;

import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.LikePost;
import com.codegym.socialmedia.model.social_action.LikePostId;
import com.codegym.socialmedia.model.social_action.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PostLikeRepository extends JpaRepository<LikePost, LikePostId> {

    // Tìm like của user cho post cụ thể
    Optional<LikePost> findByPostAndUser(Post post, User user);

    @Query("SELECT pl.id.postId FROM LikePost pl " +
            "WHERE pl.id.userId = :userId AND pl.id.postId IN :postIds")
    Set<Long> findLikedPostIdsByUser(@Param("userId") Long userId,
                                     @Param("postIds") List<Long> postIds);

    // Đếm số likes của post
    int countByPost(Post post);

    @Query("SELECT pl.post.id AS postId, COUNT(pl) AS likeCount " +
            "FROM LikePost pl " +
            "WHERE pl.post.id IN :postIds " +
            "GROUP BY pl.post.id")
    List<LikeCountProjection> countByPostIds(@Param("postIds") List<Long> postIds);
    public interface LikeCountProjection {
        Long getPostId();
        Integer getLikeCount();
    }

    // Lấy danh sách users đã like post
    @Query("SELECT pl.user FROM LikePost pl WHERE pl.post = :post")
    List<User> findUsersWhoLikedPost(@Param("post") Post post);

    // Xóa like
    void deleteByPostAndUser(Post post, User user);

    // Batch query: Check if user liked any of the posts
    @Query("SELECT pl.id.postId FROM LikePost pl " +
            "WHERE pl.id.userId = :userId AND pl.id.postId IN :postIds")
    Set<Long> findLikedPostIdsByUserOptimized(@Param("userId") Long userId,
                                              @Param("postIds") List<Long> postIds);
}
