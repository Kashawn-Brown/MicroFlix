package com.microflix.userservice.config;

import com.microflix.userservice.auth.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security setup (very light for now):
 * - Allow /auth/** and health endpoints without a token.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());      // disabling CSRF (for stateless API)
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/actuator/**", "/act/**").permitAll()     // Permitting so users can register/login and we can health-check
                .anyRequest().permitAll()       // leaving everything open for now
        );
        http.httpBasic(Customizer.withDefaults()); // not used, but harmless
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

}
