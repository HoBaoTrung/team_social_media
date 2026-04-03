package com.codegym.socialmedia.component.privacy;

import com.codegym.socialmedia.model.PrivacyLevel;
import org.springframework.stereotype.Component;

@Component
public class SpecificFriendsPrivacyPolicy implements PrivacyPolicy {
    @Override
    public PrivacyLevel level() {
        return PrivacyLevel.SPECIFIC_FRIENDS;
    }

    @Override
    public boolean canView(PrivacyContext context) {
        if (context.getSpecificFriendIds().isEmpty()) {
            return false;
        }
        return context.getSpecificFriendIds().contains(context.getViewerId());
    }
}
