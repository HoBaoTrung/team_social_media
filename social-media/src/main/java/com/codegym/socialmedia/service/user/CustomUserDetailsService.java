package com.codegym.socialmedia.service.user;

import com.codegym.socialmedia.ErrAccountException;
import com.codegym.socialmedia.model.account.AuthUser;
import com.codegym.socialmedia.model.account.Role;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.repository.user.IUserRepository;
import com.codegym.socialmedia.repository.user.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@CacheConfig(cacheNames = "userDetails")
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Cacheable(key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        IUserRepository.AuthUserProjection proj = userRepository.findAuthUserData(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));
        Set<Role> roles = roleRepository.findRolesByUsernameOrEmail(username);

        AuthUser user = new AuthUser(
                proj.getId(),
                proj.getUsername(),
                proj.getPassword(),
                proj.getIsActive(),
                proj.getAccountStatus(),
                proj.getAvatar(),
                proj.getFullName(),
                roles
        );

        // Kiểm tra trạng thái tài khoản
        if (!user.isActive()) {
            throw new ErrAccountException("Tài khoản đã bị vô hiệu hóa ");
        }

        if (user.getAccountStatus() == User.AccountStatus.BANNED) {
            throw new ErrAccountException("Tài khoản đã bị cấm ");
        }

        if (user.getAccountStatus() == User.AccountStatus.SUSPENDED) {
            throw new ErrAccountException("Tài khoản đã bị tạm khóa ");
        }
        return new CustomUserPrincipal(user);

    }
}