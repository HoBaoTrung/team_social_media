package com.codegym.socialmedia.service.friend_ship;

import com.codegym.socialmedia.dto.chat.UserSearchDto;
import com.codegym.socialmedia.dto.friend.FriendDto;
import com.codegym.socialmedia.model.PrivacyLevel;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Friendship;
import com.codegym.socialmedia.model.social_action.FriendshipId;
import com.codegym.socialmedia.model.social_action.Notification;
import com.codegym.socialmedia.repository.FriendshipRepository;
import com.codegym.socialmedia.repository.user.IUserRepository;
import com.codegym.socialmedia.service.chat.ChatService;
import com.codegym.socialmedia.service.notification.NotificationService;
import com.codegym.socialmedia.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendshipServiceImpl implements FriendshipService {

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ChatService chatService;

    @Override
    public boolean addFriendship(User user) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || user == null || currentUser.getId().equals(user.getId())) {
            return false;
        }

        FriendshipId friendshipId = new FriendshipId();
        friendshipId.setRequesterId(currentUser.getId());
        friendshipId.setAddresseeId(user.getId());

        Friendship newFriendship = new Friendship();
        newFriendship.setId(friendshipId);
        newFriendship.setAddressee(user);
        newFriendship.setRequester(currentUser);
        newFriendship.setStatus(Friendship.FriendshipStatus.PENDING);

        try {
            friendshipRepository.save(newFriendship);
            notificationService.notify(
                    currentUser.getId(), user.getId(),
                    Notification.NotificationType.FRIEND_REQUEST,
                    user.getId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    @Override
    public boolean acceptFriendship(User user) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || user == null) {
            return false;
        }

        Friendship friendship = findByUsers(user.getId(), currentUser.getId());
        if (friendship == null || friendship.getStatus() != Friendship.FriendshipStatus.PENDING) {
            return false;
        }

        friendship.setStatus(Friendship.FriendshipStatus.ACCEPTED);
        try {
            friendshipRepository.save(friendship);
            if(user.getPrivacySettings().getAllowSendMessage()!=PrivacyLevel.PRIVATE &&
                    currentUser.getPrivacySettings().getAllowSendMessage()!=PrivacyLevel.PRIVATE)
                chatService.findOrCreatePrivateConversation(currentUser.getId(), user.getId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteFriendship(User user) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || user == null) {
            return false;
        }

        Friendship friendship = findByUsers(user.getId(), currentUser.getId());
        if (friendship == null) {
            return false;
        }

        try {
            friendshipRepository.delete(friendship);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int countMutualFriends(Long userAId, Long userBId) {
        return friendshipRepository.countMutualFriends(userAId, userBId);
    }

    @Override
    public Page<FriendDto> getVisibleFriendList(User targetUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        User viewer = userService.getCurrentUser();
        if (viewer == null || targetUser == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        boolean isOwner = viewer.getId().equals(targetUser.getId());
        PrivacyLevel level = targetUser.getPrivacySettings().getShowFriendList();
        boolean isFriend = getFriendshipStatus(targetUser, viewer) == Friendship.FriendshipStatus.ACCEPTED;

        // PRIVATE: chỉ chính chủ được xem hoặc bạn bè xem bạn chung
        if (level == PrivacyLevel.PRIVATE) {
            if (isOwner) {
                return getFriendsPage(targetUser.getId(), viewer.getId(), pageable);
            } else if (isFriend) {
                return findMutualFriends(targetUser.getId(), viewer.getId(), page, size);
            }
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // FRIENDS: chỉ bạn bè hoặc chính chủ được xem
        if (level == PrivacyLevel.FRIENDS && !(isFriend || isOwner)) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // PUBLIC hoặc đủ điều kiện: lấy danh sách bạn bè
        return getFriendsPage(targetUser.getId(), viewer.getId(), pageable);
    }
    private Page<FriendDto> getFriendsPage(Long targetUserId, Long viewerId, Pageable pageable) {
        Page<User> friendsPage = friendshipRepository.findFriendsOfUserExcludingViewer(targetUserId, viewerId, pageable);

        List<Long> friendIds = friendsPage.getContent().stream()
                .filter(user -> !user.getId().equals(viewerId))
                .map(User::getId)
                .collect(Collectors.toList());

        // Batch query: Get mutual friends count for all users in one query (Fix N+1)
        Map<Long, Long> mutualFriendsCountMap = friendshipRepository
                .countMutualFriendsForMultipleUsers(viewerId, friendIds)
                .stream()
                .collect(Collectors.toMap(
                        FriendshipRepository.MutualFriendsCount::getUserId,
                        FriendshipRepository.MutualFriendsCount::getMutualCount
                ));

        List<FriendDto> friendDtos = friendsPage.getContent().stream()
                .filter(user -> !user.getId().equals(viewerId))
                .map(user -> new FriendDto(user, mutualFriendsCountMap.getOrDefault(user.getId(), 0L).intValue()))
                .collect(Collectors.toList());

        return new PageImpl<>(friendDtos, pageable, friendsPage.getTotalElements());
    }

    @Override
    public Friendship findByUsers(Long userId1, Long userId2) {
        return friendshipRepository.findByRequesterIdAndAddresseeId(userId1, userId2)
                .or(() -> friendshipRepository.findByRequesterIdAndAddresseeId(userId2, userId1))
                .orElse(null);
    }

    @Override
    public Set<Long> findFriendIdsOfUser(Long userId) {
        List<Friendship> friendships = friendshipRepository.findAllFriendshipsOfUser(userId);
        return friendships.stream()
                .map(f -> f.getRequester().getId().equals(userId) ? f.getAddressee().getId() : f.getRequester().getId())
                .collect(Collectors.toSet());
    }

    @Override
    public Page<FriendDto> findMutualFriends(Long userAId, Long userBId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> mutualFriendsPage = friendshipRepository.findMutualFriends(userAId, userBId, pageable);
        long currentUserID = userService.getCurrentUser().getId();

        List<Long> mutualFriendIds = mutualFriendsPage.getContent().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // Batch query: Get mutual friends count for all users in one query (Fix N+1)
        Map<Long, Long> mutualFriendsCountMap = mutualFriendIds.isEmpty() ?
                Collections.emptyMap() :
                friendshipRepository.countMutualFriendsForMultipleUsers(currentUserID, mutualFriendIds)
                        .stream()
                        .collect(Collectors.toMap(
                                FriendshipRepository.MutualFriendsCount::getUserId,
                                FriendshipRepository.MutualFriendsCount::getMutualCount
                        ));

        List<FriendDto> friendDtos = mutualFriendsPage.getContent().stream()
                .map(user -> new FriendDto(user, mutualFriendsCountMap.getOrDefault(user.getId(), 0L).intValue()))
                .collect(Collectors.toList());

        return new PageImpl<>(friendDtos, pageable, mutualFriendsPage.getTotalElements());
    }

    @Override
    public Page<User> findFriendsWithAllowSendMessage(User u, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        long currentUserID = userService.getCurrentUser().getId();
        return friendshipRepository.findFriendsWithAllowSendMessage(u.getId(),pageable );
    }

    @Override
    public int countFriends(Long userId) {
        return friendshipRepository.countFriendsByUserId(userId);
    }

    @Override
    public Friendship.FriendshipStatus getFriendshipStatus(User user1, User user2) {
        if (user1 == null || user2 == null || user1.equals(user2)) {
            return Friendship.FriendshipStatus.NONE;
        }

        Optional<Friendship> optional = friendshipRepository.findFriendshipBetween(user1, user2);
        if (!optional.isPresent()) {
            return Friendship.FriendshipStatus.NONE;
        }

        return optional.get().getStatus();
    }


    @Override
    public Page<FriendDto> findNonFriends(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> nonFriendsPage = friendshipRepository.findNonFriends(currentUserId, pageable);

        List<Long> nonFriendIds = nonFriendsPage.getContent().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // Batch query: Get mutual friends count for all users in one query (Fix N+1)
        Map<Long, Long> mutualFriendsCountMap = nonFriendIds.isEmpty() ?
                Collections.emptyMap() :
                friendshipRepository.countMutualFriendsForMultipleUsers(currentUserId, nonFriendIds)
                        .stream()
                        .collect(Collectors.toMap(
                                FriendshipRepository.MutualFriendsCount::getUserId,
                                FriendshipRepository.MutualFriendsCount::getMutualCount
                        ));

        List<FriendDto> friendDtos = nonFriendsPage.getContent().stream()
                .map(user -> new FriendDto(user, mutualFriendsCountMap.getOrDefault(user.getId(), 0L).intValue()))
                .collect(Collectors.toList());

        return new PageImpl<>(friendDtos, pageable, nonFriendsPage.getTotalElements());
    }

    @Override
    public Page<FriendDto> findSentFriendRequests(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> sentRequestsPage = friendshipRepository.findSentFriendRequests(currentUserId, pageable);

        List<Long> requestUserIds = sentRequestsPage.getContent().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // Batch query: Get mutual friends count for all users in one query (Fix N+1)
        Map<Long, Long> mutualFriendsCountMap = requestUserIds.isEmpty() ?
                Collections.emptyMap() :
                friendshipRepository.countMutualFriendsForMultipleUsers(currentUserId, requestUserIds)
                        .stream()
                        .collect(Collectors.toMap(
                                FriendshipRepository.MutualFriendsCount::getUserId,
                                FriendshipRepository.MutualFriendsCount::getMutualCount
                        ));

        List<FriendDto> friendDtos = sentRequestsPage.getContent().stream()
                .map(user -> new FriendDto(user, mutualFriendsCountMap.getOrDefault(user.getId(), 0L).intValue()))
                .collect(Collectors.toList());

        return new PageImpl<>(friendDtos, pageable, sentRequestsPage.getTotalElements());
    }

    @Override
    public Page<FriendDto> findReceivedFriendRequests(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> receivedRequestsPage = friendshipRepository.findReceivedFriendRequests(currentUserId, pageable);

        List<Long> requestUserIds = receivedRequestsPage.getContent().stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // Batch query: Get mutual friends count for all users in one query (Fix N+1)
        Map<Long, Long> mutualFriendsCountMap = requestUserIds.isEmpty() ?
                Collections.emptyMap() :
                friendshipRepository.countMutualFriendsForMultipleUsers(currentUserId, requestUserIds)
                        .stream()
                        .collect(Collectors.toMap(
                                FriendshipRepository.MutualFriendsCount::getUserId,
                                FriendshipRepository.MutualFriendsCount::getMutualCount
                        ));

        List<FriendDto> friendDtos = receivedRequestsPage.getContent().stream()
                .map(user -> new FriendDto(user, mutualFriendsCountMap.getOrDefault(user.getId(), 0L).intValue()))
                .collect(Collectors.toList());

        return new PageImpl<>(friendDtos, pageable, receivedRequestsPage.getTotalElements());
    }

    @Override
    public List<Friendship> findAllFriendshipsOfUser(Long userId) {
        return friendshipRepository.findAllFriendshipsOfUser(userId);
    }

    @Override
    public List<UserSearchDto> searchFriends(String keyword, Long currentUserId) {

            // Tìm trong danh sách bạn bè thay vì tất cả users
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser == null) return List.of();

            Page<User> friends = findFriendsWithAllowSendMessage(currentUser, 0, 20);

            return friends.getContent().stream()
                    .filter(u -> {
                        String fullName = safeFullName(u).toLowerCase();
                        String username = (u.getUsername() != null ? u.getUsername() : "").toLowerCase();
                        String searchTerm = keyword.toLowerCase();
                        return fullName.contains(searchTerm) || username.contains(searchTerm);
                    })
                    .map(u -> {
                        UserSearchDto dto = new UserSearchDto();
                        dto.setId(u.getId());
                        dto.setUsername(u.getUsername());
                        dto.setFullName(safeFullName(u));
                        dto.setAvatarUrl(u.getProfilePicture());
                        dto.setFriend(true);
                        return dto;
                    })
                    .collect(Collectors.toList());

    }

    @Override
    public Map<Long, Friendship.FriendshipStatus> getFriendshipMap(User currentUser, List<Long> postOwnerIds) {
        if (postOwnerIds == null || postOwnerIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Loại bỏ trùng lặp và currentUser (không cần check bản thân)
        List<Long> targetIds = postOwnerIds.stream()
                .distinct()
                .filter(id -> !id.equals(currentUser.getId()))
                .collect(Collectors.toList());

        if (targetIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Friendship> friendships = friendshipRepository
                .findFriendshipsBetweenUserAndOthers(currentUser.getId(), targetIds);

        // Tạo map mặc định là NOT_FRIEND
        Map<Long, Friendship.FriendshipStatus> friendshipMap = targetIds.stream()
                .collect(Collectors.toMap(id -> id, id -> Friendship.FriendshipStatus.NONE));

        // Cập nhật status thật từ database
        for (Friendship f : friendships) {
            Long otherId = f.getAddressee().getId().equals(currentUser.getId())
                    ? f.getRequester().getId()
                    : f.getAddressee().getId();

            friendshipMap.put(otherId, f.getStatus());
        }

        return friendshipMap;
    }


    private String safeFullName(User u) {
        String f = Optional.ofNullable(u.getFirstName()).orElse("");
        String l = Optional.ofNullable(u.getLastName()).orElse("");
        String full = (f + " " + l).trim();
        return full.isEmpty() ? Optional.ofNullable(u.getUsername()).orElse("Người dùng") : full;
    }
}