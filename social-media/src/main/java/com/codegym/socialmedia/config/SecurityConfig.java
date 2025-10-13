package com.codegym.socialmedia.config;

import com.codegym.socialmedia.component.CustomAuthFailureHandler;
import com.codegym.socialmedia.component.CustomLogoutHandler;
import com.codegym.socialmedia.jwt.JwtAuthenticationFilter;
import com.codegym.socialmedia.jwt.JwtUtil;
import com.codegym.socialmedia.model.account.User;
import com.codegym.socialmedia.repository.user.IUserRepository;
import com.codegym.socialmedia.service.user.CustomOAuth2User;
import com.codegym.socialmedia.service.user.CustomOAuth2UserService;
import com.codegym.socialmedia.service.user.CustomUserDetailsService;
import com.codegym.socialmedia.service.user.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;
import java.io.IOException;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private CustomOAuth2UserService oauth2UserService;
    @Autowired
    private CustomLogoutHandler customLogoutHandler;
    @Autowired
    private IUserRepository userRepository;

    @Autowired private UserSessionService userSessionService;

    @Autowired
    private CustomAuthFailureHandler customAuthFailureHandler;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private DataSource dataSource; // Cần thiết cho Remember Me

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl repo = new JdbcTokenRepositoryImpl();
        repo.setDataSource(dataSource);
        return repo;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")

                // --- Chính sách header (cho ảnh Google, Facebook, Cloudinary,...) ---
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("img-src 'self' https://lh3.googleusercontent.com https://*.fbcdn.net https://res.cloudinary.com https://graph.facebook.com https://i.imgur.com https://secure.gravatar.com data: blob:;")
                        )
                )

                // --- Phân quyền ---
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/login", "/register", "/css/**", "/js/**", "/images/**", "/api/debug/**", "/ws/**").permitAll()
                        .anyRequest().authenticated()
                )

                // --- Form login ---
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            handleLoginSuccess(request,response, authentication);
                        })
                        .failureHandler(customAuthFailureHandler)
                        .permitAll()
                )

                // --- OAuth2 login (Google, Facebook) ---
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(oauth2UserService))
                        .successHandler((request, response, authentication) -> {
                            handleLoginSuccess(request,response, authentication);
                        })
                        .failureHandler(customAuthFailureHandler)
                )

                // --- Remember Me ---
                .rememberMe(remember -> remember
                        .tokenRepository(persistentTokenRepository())
                        .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 ngày
                        .userDetailsService(userDetailsService)
                )

                // --- Logout ---
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(customLogoutHandler)
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "jwt_token")
                        .permitAll()
                )

                // --- CSRF ---
                .csrf(csrf -> csrf.disable())

                // --- Session ---
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        // --- Thêm JWT Filter trước UsernamePasswordAuthenticationFilter ---
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void handleLoginSuccess(HttpServletRequest request,HttpServletResponse response, Authentication authentication) throws IOException {
        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof CustomOAuth2User) {
            username = ((CustomOAuth2User) principal).getUsername();
        } else {
            throw new IllegalStateException("Unknown principal type: " + principal.getClass());
        }

        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalStateException("User not found: " + username);
        }
        userSessionService.createSession(user, request, response);

        response.sendRedirect("/news-feed");
    }
}
