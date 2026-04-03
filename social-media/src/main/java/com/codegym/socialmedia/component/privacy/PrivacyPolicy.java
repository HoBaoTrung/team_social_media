package com.codegym.socialmedia.component.privacy;

import com.codegym.socialmedia.model.PrivacyLevel;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Post;

public interface PrivacyPolicy {
    PrivacyLevel level();

    boolean canView(PrivacyContext context);

    @Deprecated(forRemoval = true)
    default boolean canView(long viewerId, long ownerId, boolean isFriend) {
        return canView(new PrivacyContext(viewerId, ownerId, isFriend));
    }

}
