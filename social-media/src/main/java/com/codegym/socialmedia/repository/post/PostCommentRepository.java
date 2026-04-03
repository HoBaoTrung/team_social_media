// PostCommentRepository.java
package com.codegym.socialmedia.repository.post;

import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Post;
import com.codegym.socialmedia.model.social_action.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    @Query("SELECT pc.post.id AS postId, COUNT(pc.id) AS commentCount " +
            "FROM PostComment pc " +
            "WHERE pc.post.id IN :postIds " +
            "AND pc.isDeleted = false " +
            "GROUP BY pc.post.id")
    List<CommentCountProjection> countByPostIds(@Param("postIds") List<Long> postIds);
    public interface CommentCountProjection {
        Long getPostId();
        Long getCommentCount();
    }

    // Lấy comments của post
    @Query("""
                SELECT pc FROM PostComment pc
                WHERE pc.post = :post
                AND pc.parent IS NULL
                AND pc.isDeleted = false
                ORDER BY pc.createdAt ASC
            """)
    Page<PostComment> findByPostOrderByCreatedAtAsc(@Param("post") Post post, Pageable pageable);

    // Đếm số comments của post
    @Query("""
                SELECT COUNT(pc) FROM PostComment pc
                WHERE pc.post = :post
                AND pc.isDeleted = false
            """)
    int countByPost(@Param("post") Post post);

    // Tìm comment theo ID và user (để kiểm tra quyền sở hữu)
    Optional<PostComment> findByIdAndUser(Long id, User user);

    // Lấy comments gần đây nhất của post
    @Query("""
                SELECT pc FROM PostComment pc
                WHERE pc.post = :post
                AND pc.parent IS NULL
                AND pc.isDeleted = false
                ORDER BY pc.createdAt DESC
            """)
    Page<PostComment> findRecentCommentsByPost(@Param("post") Post post, Pageable pageable);

    Page<PostComment> findByParent(PostComment parent, Pageable pageable);

    List<PostComment> findByParent(PostComment parent);

    // Batch query: Count comments for multiple posts
    @Query("SELECT pc.post.id AS postId, COUNT(pc.id) AS commentCount " +
            "FROM PostComment pc " +
            "WHERE pc.post.id IN :postIds " +
            "AND pc.isDeleted = false " +
            "GROUP BY pc.post.id")
    List<CommentCountProjection> countByPostIdsOptimized(@Param("postIds") List<Long> postIds);
}