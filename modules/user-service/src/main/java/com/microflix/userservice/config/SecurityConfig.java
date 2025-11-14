package com.microflix.userservice.config;

import com.microflix.userservice.auth.JwtService;
import com.microflix.userservice.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security setup:
 * - Allow /auth/** and health endpoints without a token.
 */

// enables @PreAuthorize on controllers/services
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    // Setting security rules + JWT wiring
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        http.csrf(csrf -> csrf.disable());                              // disabling CSRF (for stateless API)
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/actuator/**", "/act/**").permitAll()     // Permitting so users can register/login and we can health-check
                .requestMatchers("/api/**").authenticated()                             // Everything else under /api/** will require authentication
                .anyRequest().permitAll()                                                        // leaving everything else open for now
        );

        // run JWT filter before Spring's username/password auth filter in security filter chain (just need to happen early -> before Spring decides authorization)
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Register PasswordEncoder bean to hash passwords securely
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt is the default for passwords
    }

    // Retrieving info from yml file
    @Bean
    public JwtService jwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.ttl-minutes}") long ttlMinutes
    ) {
        return new JwtService(secret, issuer, ttlMinutes);
    }

    // Making the filter a bean so Spring can inject it
    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwt) {
        return new JwtAuthFilter(jwt);
    }

}
