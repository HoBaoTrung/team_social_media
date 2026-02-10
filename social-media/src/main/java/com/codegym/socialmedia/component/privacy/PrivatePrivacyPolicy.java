package com.codegym.socialmedia.component.privacy;

import com.codegym.socialmedia.model.PrivacyLevel;
import com.codegym.socialmedia.model.account.User;
import org.springframework.stereotype.Component;

@Component
public class PrivatePrivacyPolicy implements PrivacyPolicy {
    @Override
    public boolean canView(User viewer, User owner, boolean isFriend) {
        return false;
    }

    @Override
    public PrivacyLevel level() {
        return PrivacyLevel.PRIVATE;
    }
}
