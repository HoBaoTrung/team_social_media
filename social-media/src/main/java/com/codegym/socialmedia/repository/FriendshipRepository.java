package com.codegym.socialmedia.repository;

import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Friendship;
import com.codegym.socialmedia.model.social_action.FriendshipId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {
    Optional<Friendship> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    //  Lấy danh sách User chưa kết bạn (không có Friendship hoặc status != ACCEPTED)
    @Query("""
                SELECT u
                FROM User u
                WHERE u.id != :currentUserId
                AND u.id NOT IN (
                    SELECT f.requester.id
                    FROM Friendship f
                    WHERE f.addressee.id = :currentUserId
                    UNION
                    SELECT f.addressee.id
                    FROM Friendship f
                    WHERE f.requester.id = :currentUserId
                )
            """)
    Page<User> findNonFriends(@Param("currentUserId") Long currentUserId, Pageable pageable);


    //  Lấy danh sách User mà currentUserId đã gửi lời mời kết bạn (PENDING, currentUserId là requester)
    @Query("""
                SELECT u
                FROM User u
                WHERE u.id IN (
                    SELECT f.addressee.id
                    FROM Friendship f
                    WHERE f.requester.id = :currentUserId AND f.status = 'PENDING'
                )
            """)
    Page<User> findSentFriendRequests(@Param("currentUserId") Long currentUserId, Pageable pageable);

    //  Lấy danh sách User đã gửi lời mời kết bạn đến currentUserId (PENDING, currentUserId là addressee)
    @Query("""
                SELECT u
                FROM User u
                WHERE u.id IN (
                    SELECT f.requester.id
                    FROM Friendship f
                    WHERE f.addressee.id = :currentUserId AND f.status = 'PENDING'
                )
            """)
    Page<User> findReceivedFriendRequests(@Param("currentUserId") Long currentUserId, Pageable pageable);


    @Query("""
            SELECT u FROM User u
            WHERE u.id IN (
                SELECT f.requester.id FROM Friendship f
                WHERE f.status = 'ACCEPTED'
                  AND f.addressee.id = :targetUserId
                  AND f.requester.id <> :viewerId
                UNION
                SELECT f.addressee.id FROM Friendship f
                WHERE f.status = 'ACCEPTED'
                  AND f.requester.id = :targetUserId
                  AND f.addressee.id <> :viewerId
            )
            """)
    Page<User> findFriendsOfUserExcludingViewer(@Param("targetUserId") Long targetUserId,
                                                @Param("viewerId") Long viewerId, Pageable pageable);

    @Query("""
    SELECT u FROM User u
    JOIN u.privacySettings ps
    WHERE u.id IN (
        SELECT f.requester.id FROM Friendship f
        WHERE f.status = 'ACCEPTED'
          AND f.addressee.id = :viewerId
        UNION
        SELECT f.addressee.id FROM Friendship f
        WHERE f.status = 'ACCEPTED'
          AND f.requester.id = :viewerId
    )
    AND (
        ps.allowSendMessage = 'PUBLIC'
        OR ps.allowSendMessage = 'FRIENDS'
        OR (ps.allowSendMessage = 'PRIVATE' AND u.id = :viewerId)
    )
""")
    Page<User> findFriendsWithAllowSendMessage(@Param("viewerId") Long viewerId, Pageable pageable);



    @Query("""
                SELECT f FROM Friendship f
                WHERE f.status = 'ACCEPTED'
                  AND (f.requester.id = :userId OR f.addressee.id = :userId)
            """)
    List<Friendship> findAllFriendshipsOfUser(@Param("userId") Long userId);

    @Query(value = """
    SELECT 
        f.requester_id AS requesterId,
        f.addressee_id AS addresseeId,
        f.status
    FROM friendships f
    WHERE f.status = 'ACCEPTED'
      AND (f.requester_id = :userId OR f.addressee_id = :userId)
    """, nativeQuery = true)
    List<FriendIdWithStatus> findFriendIdsWithStatus(@Param("userId") Long userId);
    public interface FriendIdWithStatus {
        Long getRequesterId();
        Long getAddresseeId();
        Friendship.FriendshipStatus getStatus();

        default Long getFriendId(Long viewerId) {
            Long req = getRequesterId();
            Long add = getAddresseeId();
            if (req == null || add == null) {
                return req != null ? req : add;
            }
            return req.equals(viewerId) ? add : req;
        }
    }


    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.requester.id = :userId AND f.addressee.id IN :otherIds) " +
            "   OR (f.addressee.id = :userId AND f.requester.id IN :otherIds)")
    List<Friendship> findFriendshipsBetweenUserAndOthers(
            @Param("userId") Long userId,
            @Param("otherIds") List<Long> otherIds);

    @Query("""
                SELECT COUNT(f)
                FROM Friendship f
                WHERE f.status = 'ACCEPTED'
                  AND (f.requester.id = :userId OR f.addressee.id = :userId)
            """)
    int countFriendsByUserId(@Param("userId") Long userId);


    @Query("""
                SELECT u
                FROM User u
                WHERE u.id IN (
                    SELECT f1.requester.id
                    FROM Friendship f1
                    WHERE f1.addressee.id = :currentUserId AND f1.status = 'ACCEPTED'
                    UNION
                    SELECT f1.addressee.id
                    FROM Friendship f1
                    WHERE f1.requester.id = :currentUserId AND f1.status = 'ACCEPTED'
                )
                AND u.id IN (
                    SELECT f2.requester.id
                    FROM Friendship f2
                    WHERE f2.addressee.id = :targetUserId AND f2.status = 'ACCEPTED'
                    UNION
                    SELECT f2.addressee.id
                    FROM Friendship f2
                    WHERE f2.requester.id = :targetUserId AND f2.status = 'ACCEPTED'
                )
            """)
    Page<User> findMutualFriends(@Param("currentUserId") Long currentUserId,
                                 @Param("targetUserId") Long targetUserId
            , Pageable pageable);

    @Query("""
                SELECT COUNT(DISTINCT u.id)
                FROM User u
                WHERE u.id IN (
                    SELECT f1.requester.id
                    FROM Friendship f1
                    WHERE f1.addressee.id = :currentUserId AND f1.status = 'ACCEPTED'
                    UNION
                    SELECT f1.addressee.id
                    FROM Friendship f1
                    WHERE f1.requester.id = :currentUserId AND f1.status = 'ACCEPTED'
                )
                AND u.id IN (
                    SELECT f2.requester.id
                    FROM Friendship f2
                    WHERE f2.addressee.id = :targetUserId AND f2.status = 'ACCEPTED'
                    UNION
                    SELECT f2.addressee.id
                    FROM Friendship f2
                    WHERE f2.requester.id = :targetUserId AND f2.status = 'ACCEPTED'
                )
            """)
    int countMutualFriends(@Param("currentUserId") Long currentUserId,
                           @Param("targetUserId") Long targetUserId);


    @Query("SELECT f FROM Friendship f WHERE ((f.requester = :a AND f.addressee = :b) OR (f.requester = :b AND f.addressee = :a))")
    Optional<Friendship> findFriendshipBetween(@Param("a") User a, @Param("b") User b);
    @Query("""
    SELECT u
    FROM User u
    WHERE u.id IN (
        SELECT CASE
                   WHEN f.requester.id = :currentUserId THEN f.addressee.id
                   ELSE f.requester.id
               END
        FROM Friendship f
        WHERE (f.requester.id = :currentUserId OR f.addressee.id = :currentUserId)
          AND f.status = 'ACCEPTED'
    )
    AND (
        LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
""")
    List<User> findFriendsByKeyword(@Param("currentUserId") Long currentUserId,
                                    @Param("keyword") String keyword);

    // Batch query: Count mutual friends for multiple users at once
    @Query("""
    SELECT u.id AS userId, COUNT(DISTINCT mf.id) AS mutualCount
    FROM User u
    JOIN Friendship f1 ON (f1.requester = u OR f1.addressee = u)
    JOIN User mf ON ( (f1.requester = mf OR f1.addressee = mf) AND mf.id <> u.id )
    WHERE u.id IN :userBIds
      AND EXISTS (
          SELECT 1
          FROM Friendship fa
          WHERE (fa.requester.id = :userAId OR fa.addressee.id = :userAId)
            AND (fa.requester = mf OR fa.addressee = mf)
            AND fa.status = 'ACCEPTED'
      )
      AND f1.status = 'ACCEPTED'
    GROUP BY u.id
    """)
    List<MutualFriendsCount> countMutualFriendsForMultipleUsers(
            @Param("userAId") Long userAId,
            @Param("userBIds") List<Long> userBIds);

    public interface MutualFriendsCount {
        Long getUserId();
        Long getMutualCount();
    }

    // Batch query: Get friendship status for specific users (optimized for feed)
    @Query("""
    SELECT
        f.requester.id AS requesterId,
        f.addressee.id AS addresseeId,
        f.status
    FROM Friendship f
    WHERE f.status = 'ACCEPTED'
      AND ((f.requester.id = :userId AND f.addressee.id IN :targetUserIds)
           OR (f.addressee.id = :userId AND f.requester.id IN :targetUserIds))
    """)
    List<FriendIdWithStatus> findFriendshipStatusBatch(
            @Param("userId") Long userId,
            @Param("targetUserIds") List<Long> targetUserIds);

}
