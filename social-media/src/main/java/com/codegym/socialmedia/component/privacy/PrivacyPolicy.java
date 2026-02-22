package com.codegym.socialmedia.component.privacy;

import com.codegym.socialmedia.model.PrivacyLevel;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.social_action.Post;

public interface PrivacyPolicy {
    PrivacyLevel level();

    //for post
    boolean canView(User viewer, Post p, boolean isFriend);

    // for profile
    boolean canView(boolean isFriend);
}
