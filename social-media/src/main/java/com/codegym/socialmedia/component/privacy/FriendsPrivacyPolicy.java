package com.codegym.socialmedia.component.privacy;

import com.codegym.socialmedia.model.PrivacyLevel;
import org.springframework.stereotype.Component;

@Component
public class FriendsPrivacyPolicy implements PrivacyPolicy {
    @Override
    public PrivacyLevel level() {
        return PrivacyLevel.FRIENDS;
    }

    @Override
    public boolean canView(PrivacyContext context) {
        return context.isFriend();
    }
}
