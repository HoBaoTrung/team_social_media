package com.codegym.socialmedia.jwt;

import com.codegym.socialmedia.service.user.CustomUserDetailsService;
import com.codegym.socialmedia.service.user.TokenBlacklistService;
import com.codegym.socialmedia.service.user.UserSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        // Bỏ qua hoàn toàn việc kiểm tra token cho các endpoint public
        if (path.startsWith("/api/auth/") ||
                path.equals("/login") ||
                path.equals("/register") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/webjars/")){

            filterChain.doFilter(request, response);  // tiếp tục ngay, không làm gì cả
            return;
        }

        try {
            String accessToken = extractAccessToken(request);
            String refreshToken = extractRefreshToken(request);

            if (isValidToken(accessToken) == false) {
                handleExpiredToken(refreshToken, request, response);return;
            }
            else authenticateUser(accessToken, request);


        } catch (Exception e) {
            logger.error("Authentication error occurred", e);
            handleInvalidToken(request, response, "Authentication error");
            return;
        }

        filterChain.doFilter(request, response);
    }


    private void handleExpiredToken(String refreshToken,
                                    HttpServletRequest request,
                                    HttpServletResponse response) throws IOException {

        if (refreshToken == null) {
            handleUnauthorized(request, response, "Token expired");
            return;
        }

        String newAccessToken = userSessionService.refreshAccessTokenIfNeeded(refreshToken, response);

        if (newAccessToken != null) {
            authenticateUser(newAccessToken, request);
        } else {
            handleUnauthorized(request, response, "Session expired");
        }
    }

    private void handleInvalidToken(HttpServletRequest request,
                                    HttpServletResponse response,
                                    String message) throws IOException {
        handleUnauthorized(request, response, message);
    }


    private void authenticateUser(String token, HttpServletRequest request) {
        String username = jwtUtil.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }

    private boolean isValidToken(String token) {
        if (token == null || tokenBlacklistService.isTokenBlacklisted(token) ) {
            return false;
        }
        return jwtUtil.validateToken(token);
    }


    private String extractAccessToken(HttpServletRequest request) {

        // 1. Header (Angular)
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // 2. Cookie (Thymeleaf)
        return getCookieValue(request, "jwt_token");
    }

    private String extractRefreshToken(HttpServletRequest request) {
        return getCookieValue(request, "refresh_token");
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }


    private void handleUnauthorized(HttpServletRequest request,
                                    HttpServletResponse response,
                                    String message) throws IOException {

        if (isApiRequest(request)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
        } else {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
            response.sendRedirect("/login?error=" + encodedMessage);
        }
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api");
    }
}