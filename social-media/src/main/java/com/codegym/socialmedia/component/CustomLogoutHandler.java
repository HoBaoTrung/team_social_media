package com.codegym.socialmedia.component;

import com.codegym.socialmedia.jwt.JwtUtil;
import com.codegym.socialmedia.repository.user.UserSessionRepository;
import com.codegym.socialmedia.service.user.TokenBlacklistService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
@Component
public class CustomLogoutHandler implements LogoutHandler {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String refreshToken = extractCookieValue(request, "refresh_token");
        String jwtToken = extractCookieValue(request, "jwt_token");
        if (refreshToken != null) {
            // Tìm và xóa session theo refresh_token
            userSessionRepository.findByRefreshToken(refreshToken)
                    .ifPresent(userSessionRepository::delete);
        }

        if (jwtToken != null) {
            long expiryInSeconds = jwtUtil.getRemainingTime(jwtToken);
            if (expiryInSeconds > 0) tokenBlacklistService.blacklistToken(jwtToken, expiryInSeconds);
        }
        // Xóa cookie phía client
        clearCookie(response, "jwt_token");
        clearCookie(response, "refresh_token");
    }

    // ======= Helper methods =======

    private String extractCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void clearCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
