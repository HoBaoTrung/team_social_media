package com.codegym.socialmedia.service.user;

import com.codegym.socialmedia.jwt.JwtUtil;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.model.account.UserSession;
import com.codegym.socialmedia.repository.user.UserSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserSessionService {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Tạo mới session khi người dùng đăng nhập
     */
    public void createSession(User user, HttpServletRequest request, HttpServletResponse response) {
        String accessToken = jwtUtil.generateToken(user.getUsername()); // 10h
        String refreshToken = UUID.randomUUID().toString();             // 30 ngày

        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshToken(refreshToken);
        session.setIpAddress(request.getRemoteAddr());
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setDeviceInfo(detectDevice(request.getHeader("User-Agent")));
        session.setLoginMethod(UserSession.LoginMethod.WEB);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(30)); // refresh token hết hạn
        session.setLastActivity(LocalDateTime.now());

        userSessionRepository.save(session);

        // Cookie Access Token (10h)
        addCookie(response, "jwt_token", accessToken, 10 * 60 * 60);
        // Cookie Refresh Token (30 ngày)
        addCookie(response, "refresh_token", refreshToken, 30 * 24 * 60 * 60);
    }

    //Tạo mới session khi người dùng đăng nhập bằng api
    public Map<String, String> createRefreshToken_AccessToken(User user, HttpServletRequest request) {
        String accessToken = jwtUtil.generateToken(user.getUsername()); // 10p
        String refreshToken = UUID.randomUUID().toString();             // 30 ngày

        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshToken(refreshToken);
        session.setIpAddress(request.getRemoteAddr());
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setDeviceInfo(detectDevice(request.getHeader("User-Agent")));
        session.setLoginMethod(UserSession.LoginMethod.WEB);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(30)); // refresh token hết hạn
        session.setLastActivity(LocalDateTime.now());

        userSessionRepository.save(session);

        Map<String, String> result = new HashMap<>();
        result.put("access_token", accessToken);
        result.put("refresh_token", refreshToken);
        return result;
    }
    public String refreshAccessTokenIfNeededForApi(String refreshToken, HttpServletResponse response) {
        Optional<UserSession> sessionOpt = userSessionRepository.findByRefreshToken(refreshToken);
        if (sessionOpt.isEmpty()) return null;

        UserSession session = sessionOpt.get();

        // RefreshToken hết hạn?
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            logoutSession(refreshToken, response);
            return null;
        }

        // Sinh access token mới
        String username = session.getUser().getUsername();
        String newAccessToken = jwtUtil.generateToken(username);

        return newAccessToken;
    }

    /**
     * Làm mới accessToken khi accessToken hết hạn nhưng refreshToken còn hạn
     */
    public String refreshAccessTokenIfNeeded(String refreshToken, HttpServletResponse response) {
        Optional<UserSession> sessionOpt = userSessionRepository.findByRefreshToken(refreshToken);
        if (sessionOpt.isEmpty()) return null;

        UserSession session = sessionOpt.get();

        // RefreshToken hết hạn?
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            logoutSession(refreshToken, response);
            return null;
        }

        // Sinh access token mới
        String username = session.getUser().getUsername();
        String newAccessToken = jwtUtil.generateToken(username);

        // Cập nhật cookie
        addCookie(response, "jwt_token", newAccessToken, 10 * 60 * 60);

        return newAccessToken;
    }

    /**
     * ✅ Cập nhật thời điểm hoạt động cuối khi người dùng truy cập
     */
    public void updateLastActivity(String token) {
        userSessionRepository.findByRefreshToken(token).ifPresent(session -> {
            // Kiểm tra hạn của token
            if (session.getExpiresAt().isAfter(LocalDateTime.now())) {
                session.setLastActivity(LocalDateTime.now());
                userSessionRepository.save(session);
            } else {
                userSessionRepository.delete(session);
            }
        });
    }

    /**
     * ✅ Đăng xuất & xóa cookie
     */
    public void logoutSession(String refreshToken, HttpServletResponse response) {
        userSessionRepository.findByRefreshToken(refreshToken).ifPresent(session -> {
            userSessionRepository.delete(session);
        });

        deleteCookie(response, "jwt_token");
        deleteCookie(response, "refresh_token");
    }

    /* ------------------ Helper methods ------------------ */

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String detectDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.toLowerCase().contains("mobile")) return "Mobile";
        if (userAgent.toLowerCase().contains("tablet")) return "Tablet";
        return "Web";
    }
}