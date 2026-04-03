package com.codegym.socialmedia.component.privacy;

import com.codegym.socialmedia.model.PrivacyLevel;
import org.springframework.stereotype.Component;

@Component
public class PublicPrivacyPolicy implements PrivacyPolicy {
    @Override
    public PrivacyLevel level() {
        return PrivacyLevel.PUBLIC;
    }

    @Override
    public boolean canView(PrivacyContext context) {
        return true;
    }
}
