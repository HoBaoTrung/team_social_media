package com.codegym.socialmedia.jwt;

import com.codegym.socialmedia.service.user.CustomUserDetailsService;
import com.codegym.socialmedia.service.user.TokenBlacklistService;
import com.codegym.socialmedia.service.user.UserSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String jwtToken = getCookieValue(request, "jwt_token");
        String refreshToken = getCookieValue(request, "refresh_token");

        try {
            if (jwtToken != null) {
                if (jwtUtil.validateToken(jwtToken)) {
                    if (tokenBlacklistService.isTokenBlacklisted(jwtToken)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has been logged out");
                        return;
                    }

                    // ✅ Token còn hạn → xác thực bình thường
                    authenticateUser(jwtToken, request);
                    userSessionService.updateLastActivity(refreshToken);
                } else if (jwtUtil.isTokenExpired(jwtToken)) {
                    // ⚠️ Access token hết hạn → thử refresh
                    String newAccessToken = userSessionService.refreshAccessTokenIfNeeded(refreshToken, response);
                    if (newAccessToken != null) {
                        authenticateUser(newAccessToken, request);
                    } else {
                        // ❌ Refresh token cũng hết hạn → buộc đăng nhập lại
                        response.sendRedirect("/login?expired");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("/login?error=invalid");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(String token, HttpServletRequest request) {
        String username = jwtUtil.extractUsername(token);
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }
}