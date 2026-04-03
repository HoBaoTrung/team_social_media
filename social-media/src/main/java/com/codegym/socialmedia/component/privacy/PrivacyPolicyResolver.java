package com.codegym.socialmedia.component.privacy;

import com.codegym.socialmedia.model.PrivacyLevel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
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
            long viewerId,
            long ownerId,
            PrivacyLevel level,
            boolean isFriend
    ) {
        return canView(viewerId, ownerId, level, isFriend, Set.of(), Set.of(), Set.of());
    }

    public boolean canView(
            long viewerId,
            long ownerId,
            PrivacyLevel level,
            boolean isFriend,
            Set<Long> friendIds,
            Set<Long> excludedUserIds,
            Set<Long> specificFriendIds
    ) {
        if (level == null) return false;
        if (viewerId == ownerId) return true;
        if (viewerId == -1L) return false;

        PrivacyContext context = new PrivacyContext(
                viewerId,
                ownerId,
                isFriend,
                friendIds,
                excludedUserIds,
                specificFriendIds
        );

        PrivacyPolicy policy = policyMap.get(level);
        return policy != null && policy.canView(context);
    }
}

