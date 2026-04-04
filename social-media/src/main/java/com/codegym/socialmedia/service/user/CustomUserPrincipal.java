package com.codegym.socialmedia.service.user;

import com.codegym.socialmedia.general_interface.UserPrincipalInfo;
import com.codegym.socialmedia.model.account.AuthUser;
import com.codegym.socialmedia.model.account.Role;
import com.codegym.socialmedia.model.account.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CustomUserPrincipal implements UserDetails, UserPrincipalInfo, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final AuthUser user;
    private Collection<? extends GrantedAuthority> roles;


    public CustomUserPrincipal(AuthUser user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getAccountStatus() == User.AccountStatus.ACTIVE;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }



    @Override
    public String getAvatarUrl() {
        return user.getAvatar();
    }

    @Override
    public String getFullName() {
        return user.getFullName();
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }


    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }


    public AuthUser getAuthUser() {
        return user;
    }


    public String getName() {
        return user.getUsername();
    }
}
