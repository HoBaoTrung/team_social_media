package com.codegym.socialmedia.service.user;

import com.codegym.socialmedia.ErrAccountException;
import com.codegym.socialmedia.component.upload_file.CloudinaryService;
import com.codegym.socialmedia.component.UrlMultipartFile;
import com.codegym.socialmedia.model.account.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService
        implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    private CloudinaryService cloudinaryService;

    public static MultipartFile fromUrl(String url, String fileName) throws Exception {
        try (InputStream in = new URL(url).openStream()) {
            byte[] bytes = in.readAllBytes();
            return new UrlMultipartFile(bytes, fileName, "image/jpeg");
        }
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);


            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            Map<String, Object> attributes = oauth2User.getAttributes();

            String email = null;
            String name = null;
            String avatarUrl = null;

            if ("facebook".equalsIgnoreCase(registrationId)) {
                // Facebook
                String facebookId = (String) attributes.get("id");
                email = (String) attributes.get("email");
                name = (String) attributes.get("name");
                avatarUrl = "https://graph.facebook.com/" + facebookId + "/picture?type=large";
            } else if ("google".equalsIgnoreCase(registrationId)) {
                // Google
                email = (String) attributes.get("email");
                name = (String) attributes.get("name");
                avatarUrl = (String) attributes.get("picture");
                if (avatarUrl.contains("=s96-c")) {
                    avatarUrl = avatarUrl.replace("=s96-c", "=s512-c");
                }
            }

            if (email != null && name != null) {
                // Lưu hoặc cập nhật người dùng
                userService.createOrUpdateOAuth2User(email, name, registrationId, avatarUrl);

                // Lấy thông tin người dùng từ DB
                User user = userService.findByEmail(email);
                if (user == null) {
                    throw new OAuth2AuthenticationException("User not found after saving");
                }

                String userNameAttributeName = userRequest.getClientRegistration()
                        .getProviderDetails()
                        .getUserInfoEndpoint()
                        .getUserNameAttributeName();

                // Kiểm tra trạng thái tài khoản
                if (!user.isActive()) {
                    throw new ErrAccountException("Tài khoản đã bị vô hiệu hóa ");
                }

                // Return custom OAuth2User với ảnh avatar từ DB
                return new CustomOAuth2User(
                        oauth2User.getAuthorities(),
                        attributes,
                        userNameAttributeName,
                        user.getProfilePicture(),
                        user.getFirstName() + " " + user.getLastName(),
                        user.getUsername(),
                        user.getEmail(), user.getId()
                );

            }

            throw new OAuth2AuthenticationException("Email or name missing from OAuth2 provider");

        }
    }


