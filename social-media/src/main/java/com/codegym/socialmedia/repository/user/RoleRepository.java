package com.codegym.socialmedia.repository.user;

import com.codegym.socialmedia.model.account.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);
}
