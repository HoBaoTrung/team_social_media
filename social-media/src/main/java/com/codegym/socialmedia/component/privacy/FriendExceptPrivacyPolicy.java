package com.codegym.socialmedia.component.privacy;

import com.codegym.socialmedia.model.PrivacyLevel;
import org.springframework.stereotype.Component;

@Component
public class FriendExceptPrivacyPolicy implements PrivacyPolicy {
    @Override
    public PrivacyLevel level() {
        return PrivacyLevel.FRIEND_EXCEPT;
    }

    @Override
    public boolean canView(PrivacyContext context) {
        if (!context.isFriend()) {
            return false;
        }
        return !context.getExcludedUserIds().contains(context.getViewerId());
    }
}
