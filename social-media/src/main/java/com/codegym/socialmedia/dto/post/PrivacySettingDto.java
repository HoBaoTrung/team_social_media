package com.codegym.socialmedia.dto.post;

import com.codegym.socialmedia.model.PrivacyLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PrivacySettingDto {
    private PrivacyLevel level  = PrivacyLevel.PUBLIC;

    // chỉ dùng khi SPECIFIC_FRIENDS
    private Set<Long> allowedUserIds;

    // chỉ dùng khi FRIEND_EXCEPT
    private Set<Long> excludedUserIds;
}
