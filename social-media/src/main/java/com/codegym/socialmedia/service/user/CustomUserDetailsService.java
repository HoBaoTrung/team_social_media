package com.codegym.socialmedia.service.user;

import com.codegym.socialmedia.ErrAccountException;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.repository.user.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private IUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = null;
        user = userRepository.findByEmail(username);
        if (user == null) user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("Không tìm thấy người dùng với tên đăng nhập hoặc email: " + username);
        }

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