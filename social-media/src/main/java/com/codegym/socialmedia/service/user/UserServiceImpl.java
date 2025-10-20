package com.codegym.socialmedia.service.user;

import com.codegym.socialmedia.component.CloudinaryService;
import com.codegym.socialmedia.dto.user.UserDTO;
import com.codegym.socialmedia.dto.user.UserRegistrationDto;
import com.codegym.socialmedia.model.account.NotificationSettings;
import com.codegym.socialmedia.model.account.Role;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.account.UserPrivacySettings;
import com.codegym.socialmedia.repository.*;
import com.codegym.socialmedia.repository.user.IUserRepository;
import com.codegym.socialmedia.repository.user.RoleRepository;
import com.codegym.socialmedia.repository.user.UserPrivacySettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static com.codegym.socialmedia.service.user.CustomOAuth2UserService.fromUrl;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private IUserRepository iUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private UserPrivacySettingsRepository userPrivacySettingsRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    @Qualifier("customUserDetailsService")
    private UserDetailsService userDetailsService;
    private FriendshipRepository friendshipRepository;
    public User save(UserRegistrationDto registrationDto) {
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setPhone(registrationDto.getPhone());
        user.setDateOfBirth(registrationDto.getDateOfBirth());
        user.setLoginMethod(User.LoginMethod.EMAIL);
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        user.setActive(true);
        user.setVerified(false);
        user.setProfilePicture("https://res.cloudinary.com/dryyvmkwo/image/upload/v1748588721/samples/landscapes/nature-mountains.jpg");
        // Lấy role từ DB
        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role not found"));

        // Gán role
        user.setRoles(new HashSet<>(Arrays.asList(roleUser)));
        UserPrivacySettings privacySettings = new UserPrivacySettings();
        privacySettings.setUser(user);

        NotificationSettings notificationSettings = new NotificationSettings();
        notificationSettings.setUser(user);

        user.setPrivacySettings(privacySettings);
        user.setNotificationSettings(notificationSettings);

        User savedUser = iUserRepository.save(user);
        return savedUser;
    }

    @Override
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return null;
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof UserDetails) {
            // Form login
            String username = ((UserDetails) principal).getUsername();
            return getUserByUsername(username);
        } else if (principal instanceof OAuth2User) {
            // OAuth2 login
            OAuth2User oauth2User = (OAuth2User) principal;
            String email = (String) oauth2User.getAttribute("email");
            return iUserRepository.findByEmail(email);
        }

        return null;
    }

    @Override
    public User getUserById(Long id) {
        return iUserRepository.findById(id).orElse(null);
    }

    @Override
    public User getUserByUsername(String username) {
        return iUserRepository.findByUsername(username);
    }

    @Override
    public User save(User newUser) {
        User existingUser = getUserByUsername(newUser.getUsername());
        if (existingUser == null) {
            // Người dùng mới, cần mã hóa mật khẩu
            newUser.setPasswordHash(passwordEncoder.encode(newUser.getPasswordHash()));
        } else {
            // Người dùng cũ, chỉ mã hóa nếu mật khẩu thay đổi
            if (!passwordEncoder.matches(newUser.getPasswordHash(), existingUser.getPasswordHash())) {
                newUser.setPasswordHash(passwordEncoder.encode(newUser.getPasswordHash()));
            } else {
                // Nếu mật khẩu không thay đổi thì giữ lại cái cũ
                newUser.setPasswordHash(existingUser.getPasswordHash());
            }
        }
        return iUserRepository.save(newUser);
    }

    @Override
    public User save(User user, MultipartFile image) {
        if (image != null && !image.isEmpty()) {
            user.setProfilePicture(cloudinaryService.upload(image));
        }
        return iUserRepository.save(user);
    }

    @Override
    public void refreshAuthentication(String username) {
        UserDetails updatedUser = userDetailsService.loadUserByUsername(username);

        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(
                        updatedUser,
                        updatedUser.getPassword(),
                        updatedUser.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }

    public User findByEmail(String email) {
        return iUserRepository.findByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return iUserRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return iUserRepository.existsByEmail(email);
    }

    public User createOrUpdateOAuth2User(String email, String name, String provider, String avatar) {
        User user = findByEmail(email);

        if (user == null) {
            // Tạo user mới cho OAuth2
            user = new User();
            user.setEmail(email);

            // Tạo username unique từ email
            String baseUsername = email.split("@")[0];
            String username = generateUniqueUsername(baseUsername);
            user.setUsername(username);

            try {
                MultipartFile avatarFile = fromUrl(avatar, "avatar.jpg");
                avatar = cloudinaryService.upload(avatarFile);
            } catch (Exception e) {
                e.printStackTrace();
            }

            user.setProfilePicture(avatar);
            user.setPasswordHash(""); // OAuth2 không cần password

            // Tách firstName và lastName từ name
            if (name != null && !name.isEmpty()) {
                String[] nameParts = name.split(" ", 2);
                user.setFirstName(nameParts[0]);
                if (nameParts.length > 1) {
                    user.setLastName(nameParts[1]);
                }
            }

            // Set login method dựa trên provider
            if ("google".equalsIgnoreCase(provider)) {
                user.setLoginMethod(User.LoginMethod.GOOGLE);
            } else if ("facebook".equalsIgnoreCase(provider)) {
                user.setLoginMethod(User.LoginMethod.FACEBOOK);
            } else {
                user.setLoginMethod(User.LoginMethod.EMAIL);
            }

            user.setAccountStatus(User.AccountStatus.ACTIVE);
            user.setActive(true);
            user.setVerified(true); // OAuth2 user đã được verify

            UserPrivacySettings privacySettings = new UserPrivacySettings();
            privacySettings.setUser(user);

            NotificationSettings notificationSettings = new NotificationSettings();
            notificationSettings.setUser(user);

            user.setPrivacySettings(privacySettings);
            user.setNotificationSettings(notificationSettings);

            // Lấy role từ DB
            Role roleUser = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            // Gán role
            user.setRoles(new HashSet<>(Arrays.asList(roleUser)));

            User savedUser = iUserRepository.save(user);
            return savedUser;
        } else {
            // Cập nhật thông tin user hiện có
            if (name != null && !name.isEmpty()) {
                String[] nameParts = name.split(" ", 2);
                if (user.getFirstName() == null || user.getFirstName().isEmpty()) {
                    user.setFirstName(nameParts[0]);
                }
                if (nameParts.length > 1 && (user.getLastName() == null || user.getLastName().isEmpty())) {
                    user.setLastName(nameParts[1]);
                }
            }

            // Cập nhật login method nếu chưa có
            if (user.getLoginMethod() == User.LoginMethod.EMAIL) {
                if ("google".equalsIgnoreCase(provider)) {
                    user.setLoginMethod(User.LoginMethod.GOOGLE);
                } else if ("facebook".equalsIgnoreCase(provider)) {
                    user.setLoginMethod(User.LoginMethod.FACEBOOK);
                }
            }

            return iUserRepository.save(user);
        }
    }

    private String generateUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;

        // Loại bỏ ký tự đặc biệt
        username = username.replaceAll("[^a-zA-Z0-9_]", "");

        // Đảm bảo username không trống
        if (username.isEmpty()) {
            username = "user";
        }

        // Kiểm tra và tạo username unique
        while (existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    // Thêm các method debug
    public List<User> getAllUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return iUserRepository.findAllById(ids);
    }

    public long countUsers() {
        return iUserRepository.count();
    }

    public void deleteAllUsers() {
        iUserRepository.deleteAll();
    }

    // ✅ REMOVED getUserStats method - now handled by UserStatsService
}