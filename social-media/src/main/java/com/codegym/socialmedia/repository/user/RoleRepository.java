package com.codegym.socialmedia.repository.user;

import com.codegym.socialmedia.model.account.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);

    @Query("SELECT u.roles FROM User u WHERE u.username = :username OR u.email = :username")
    Set<Role> findRolesByUsernameOrEmail(String username);
}
