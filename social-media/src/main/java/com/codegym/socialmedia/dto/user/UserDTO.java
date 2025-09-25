package com.codegym.socialmedia.dto.user;

import com.codegym.socialmedia.model.account.Role;
import com.codegym.socialmedia.model.account.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String email;
    private Set<Role> roles;
    private boolean active;

    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.fullName = user.getFirstName() + " " + user.getLastName();
        this.avatarUrl = user.getProfilePicture();
        this.email = user.getEmail();
        this.roles = user.getRoles();
        this.active = user.isActive();
    }
}
