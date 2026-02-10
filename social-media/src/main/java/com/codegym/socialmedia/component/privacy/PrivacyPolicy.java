package com.codegym.socialmedia.component.privacy;

import com.codegym.socialmedia.model.PrivacyLevel;
import com.codegym.socialmedia.model.account.User;

public interface PrivacyPolicy {
    PrivacyLevel level();
    boolean canView(User viewer, User owner, boolean isFriend);
}
