package com.microflix.userservice.security;

import com.microflix.userservice.auth.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security setup for user-service:
 * - /api/v1/auth/** and health/actuator endpoints are public.
 * - All other /api/** routes require a valid JWT.
 * - Uses stateless JWT-based authentication.
 */
@EnableMethodSecurity       // enables @PreAuthorize on controllers/services
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    // Setting security rules + JWT wiring
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable);      // No CSRF for stateless APIs
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/api/v1/users/health", "/actuator/**", "/act/**").permitAll()     // Permitting so users can register/login and we can health-check
                .requestMatchers("/api/**").authenticated()                             // Everything else under /api/** will require authentication
                .anyRequest().permitAll()                                                        // leaving everything else open for now
        );

        // Use custom handler when authentication is required but missing/invalid.
        http.exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint));

        // Run JWT filter before Spring's username/password auth filter in security filter chain
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Password encoder bean used to hash user passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt is the default for passwords
    }

    // Retrieving info from yml file
    // JwtService bean for issuing and verifying JWTs.
    @Bean
    public JwtService jwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.ttl-minutes}") long ttlMinutes
    ) {
        return new JwtService(secret, issuer, ttlMinutes);
    }

}
