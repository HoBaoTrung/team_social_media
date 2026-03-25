package com.codegym.socialmedia.controller;

import com.codegym.socialmedia.dto.authen.LoginRequest;
import com.codegym.socialmedia.jwt.JwtUtil;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.repository.user.IUserRepository;
import com.codegym.socialmedia.service.user.CustomUserPrincipal;
import com.codegym.socialmedia.service.user.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final IUserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        try {
            // Xác thực username/password
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            // Lấy UserDetails
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String username = userDetails.getUsername();

            // Tìm User entity (nếu cần thêm thông tin)
            User user = userRepository.findByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "User not found in database"));
            }

            // Generate JWT
            String jwt = jwtUtil.generateToken(username);

            // Nếu bạn vẫn muốn tạo session phụ (remember-me, websocket, etc...)
            // userSessionService.createSession(user, httpRequest, httpResponse);
            // Nhưng nếu STATELESS thì thường không cần

            // Return response
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("username", username);
            response.put("fullName", user.getFullName());
            response.put("avatarUrl", user.getProfilePicture());
            response.put("roles", userDetails.getAuthorities());

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed. Please try again later."));
        }
    }

    // Get current authenticated user endpoint
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CustomUserPrincipal userDetails = (CustomUserPrincipal) authentication.getPrincipal();

        Map<String, Object> response = new HashMap<>();
        response.put("username", userDetails.getUsername());
        response.put("roles", userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList());

        return ResponseEntity.ok(response);
    }
}