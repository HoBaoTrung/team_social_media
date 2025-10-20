package com.codegym.socialmedia.service.user;

import com.codegym.socialmedia.dto.user.UserDTO;
import com.codegym.socialmedia.dto.user.UserRegistrationDto;
import com.codegym.socialmedia.model.account.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {
    User getCurrentUser();
    void refreshAuthentication(String username);
    User getUserById(Long id);

    User getUserByUsername(String username);

    User save(User user);

    User save(User user, MultipartFile file);

    User save(UserRegistrationDto registrationDto);

    User findByEmail(String email);

    boolean existsByUsername(String username);

    User createOrUpdateOAuth2User(String email, String name, String provider, String avatar);

}