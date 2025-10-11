package com.codegym.socialmedia.config;

import com.codegym.socialmedia.component.CustomAuthFailureHandler;
import com.codegym.socialmedia.jwt.JwtAuthenticationFilter;
import com.codegym.socialmedia.jwt.JwtUtil;
import com.codegym.socialmedia.service.user.CustomOAuth2UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService oauth2UserService;

    @Autowired
    private CustomAuthFailureHandler customAuthFailureHandler;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtUtil jwtUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain userFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("img-src 'self' https://lh3.googleusercontent.com https://*.fbcdn.net https://res.cloudinary.com https://graph.facebook.com https://i.imgur.com https://secure.gravatar.com data: blob:;")
                        )
                )
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**", "/api/debug/**", "/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            actionSuccessHandler(response, authentication);
                        })
                        .failureHandler(customAuthFailureHandler)
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService))
                        .successHandler((request, response, authentication) -> {
                            actionSuccessHandler(response, authentication);
                        })
                        .failureHandler(customAuthFailureHandler)
                )

                // --- Logout ---
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "jwt_token")
                        .permitAll()
                )

                // --- Vô hiệu hoá CSRF (cho JWT) ---
                .csrf(csrf -> csrf.disable())

                // --- Cho phép stateless (JWT) ---
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // --- Thêm JWT Filter trước UsernamePasswordAuthenticationFilter ---
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void actionSuccessHandler(HttpServletResponse response, Authentication authentication) throws IOException {
        String token = jwtUtil.generateToken(
                (UserDetails) authentication.getPrincipal()
        );

        // Gửi JWT về client
        Cookie cookie = new Cookie("jwt_token", token);
        cookie.setHttpOnly(true);   // Bảo vệ khỏi XSS
//        cookie.setSecure(true);     // Chỉ gửi qua HTTPS (bắt buộc nếu deploy)
        cookie.setPath("/");
        cookie.setMaxAge(10 * 60 * 60); // 10 giờ
        response.addCookie(cookie);
        response.sendRedirect("/news-feed");
    }
}
