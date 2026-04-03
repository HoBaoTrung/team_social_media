package com.codegym.socialmedia.component.privacy;

import java.util.Set;

public class PrivacyContext {
    private final long viewerId;
    private final long ownerId;
    private final boolean isFriend;
    private final Set<Long> friendIds;
    private final Set<Long> excludedUserIds;
    private final Set<Long> specificFriendIds;

    public PrivacyContext(long viewerId, long ownerId, boolean isFriend) {
        this(viewerId, ownerId, isFriend, Set.of(), Set.of(), Set.of());
    }

    public PrivacyContext(
            long viewerId,
            long ownerId,
            boolean isFriend,
            Set<Long> friendIds,
            Set<Long> excludedUserIds,
            Set<Long> specificFriendIds
    ) {
        this.viewerId = viewerId;
        this.ownerId = ownerId;
        this.isFriend = isFriend;
        this.friendIds = friendIds;
        this.excludedUserIds = excludedUserIds;
        this.specificFriendIds = specificFriendIds;
    }

    public long getViewerId() {
        return viewerId;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public boolean isFriend() {
        return isFriend;
    }

    public Set<Long> getFriendIds() {
        return friendIds;
    }

    public Set<Long> getExcludedUserIds() {
        return excludedUserIds;
    }

    public Set<Long> getSpecificFriendIds() {
        return specificFriendIds;
    }

    public boolean isOwner() {
        return viewerId != -1L && ownerId != -1L && viewerId == ownerId;
    }
}
