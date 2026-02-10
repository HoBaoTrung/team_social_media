package com.codegym.socialmedia.component.privacy;

import com.codegym.socialmedia.model.PrivacyLevel;
import com.codegym.socialmedia.model.account.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
@Component
public class PrivacyPolicyResolver {

    private final Map<PrivacyLevel, PrivacyPolicy> policyMap;

    public PrivacyPolicyResolver(List<PrivacyPolicy> policies) {
        this.policyMap = policies.stream()
                .collect(Collectors.toMap(
                        PrivacyPolicy::level,
                        Function.identity()
                ));
    }

    public boolean canView(
            User viewer,
            User owner,
            PrivacyLevel level,
            boolean isFriend
    ) {
        if (viewer == null || owner == null || level == null) return false;
        if (viewer.getId().equals(owner.getId())) return true;

        PrivacyPolicy policy = policyMap.get(level);
        return policy != null && policy.canView(viewer, owner, isFriend);
    }
}
