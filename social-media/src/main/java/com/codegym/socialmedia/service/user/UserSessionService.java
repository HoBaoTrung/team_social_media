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
import java.util.Optional;
import java.util.UUID;

@Service
public class UserSessionService {

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public void createSession(User user, HttpServletRequest request, HttpServletResponse response) {
        String accessToken = jwtUtil.generateToken(user.getUsername()); // 10h
        String refreshToken = UUID.randomUUID().toString(); // 30 ngày

        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionToken(accessToken);
        session.setRefreshToken(refreshToken);
        session.setIpAddress(request.getRemoteAddr());
        session.setUserAgent(request.getHeader("User-Agent"));
        session.setDeviceInfo(detectDevice(request.getHeader("User-Agent")));
        session.setLoginMethod(UserSession.LoginMethod.WEB);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(30));
        session.setLastActivity(LocalDateTime.now());
        session.setActive(true);
        userSessionRepository.save(session);

        // Cookie Access Token
        Cookie jwtCookie = new Cookie("jwt_token", accessToken);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(10 * 60 * 60); // 10h

        // Cookie Refresh Token
        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(30 * 24 * 60 * 60); // 30 ngày

        response.addCookie(jwtCookie);
        response.addCookie(refreshCookie);
    }


    public boolean validateSession(String token) {
        Optional<UserSession> sessionOpt = userSessionRepository.findBySessionTokenAndIsActiveTrue(token);
        if (sessionOpt.isEmpty()) return false;

        UserSession session = sessionOpt.get();
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            userSessionRepository.delete(session);
            return false;
        }
        return true;
    }


    private String detectDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobile")) return "Mobile";
        if (userAgent.contains("Tablet")) return "Tablet";
        return "Web";
    }
}
