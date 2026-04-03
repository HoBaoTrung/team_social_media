package com.codegym.socialmedia.model.account;

import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.account.Role;
import java.util.Set;

public class AuthUser {

    private  Long id;
    private  String username;
    private  String password;
    private  boolean isActive;
    private  User.AccountStatus accountStatus;
    private  String avatar;
    private  String fullName;
    private  Set<Role> roles;
    private boolean isAdmin;

    public AuthUser(Long id, String username, String password, boolean isActive, User.AccountStatus accountStatus, String avatar, String fullName, Set<Role> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.isActive = isActive;
        this.accountStatus = accountStatus;
        this.avatar = avatar;
        this.fullName = fullName;
        this.roles = roles;
        this.isAdmin = roles != null &&
                roles.stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));;

    }

    public AuthUser() {
    }

    public boolean isAdmin() {
        return this.isAdmin;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isActive() {
        return isActive;
    }

    public User.AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getFullName() {
        return fullName;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}