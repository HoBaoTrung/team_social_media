package com.codegym.socialmedia.dto.post;

import com.codegym.socialmedia.model.PrivacyLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public abstract class BasePrivacyDto {

    @NotNull(message = "Privacy level is required")
    private PrivacyLevel privacyLevel;

    /**
     * Dùng cho SPECIFIC_FRIENDS
     */
    private Set<Long> allowedUserIds = new HashSet<>();

    /**
     * Dùng cho FRIEND_EXCEPT
     */
    private Set<Long> excludedUserIds = new HashSet<>();

    public boolean isSpecificFriends() {
        return privacyLevel == PrivacyLevel.SPECIFIC_FRIENDS;
    }

    public boolean isFriendExcept() {
        return privacyLevel == PrivacyLevel.FRIEND_EXCEPT;
    }

    public boolean isCustomPrivacy() {
        return isSpecificFriends() || isFriendExcept();
    }
}