package com.microflix.rating_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Minimal stateless security config using JWT.
// Protects write operations; read operations are public for now.
@EnableMethodSecurity       // enables @PreAuthorize on controllers/services (not implemented yet)
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    JwtAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, JwtAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)      // No CSRF needed for stateless APIs

                // We don't use HTTP sessions; every request must carry its own auth (JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Permitting Health/info endpoints
                        .requestMatchers("/actuator/**").permitAll()

                        // Write operations require authentication
                        .requestMatchers(HttpMethod.POST, "/api/v1/ratings").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/ratings").authenticated()


                        .requestMatchers("/api/v1/engagements/**").authenticated()       // Need to be authenticated to manipulate lists

                        // Read operations on ratings are public for now
                        .requestMatchers(HttpMethod.GET, "/api/v1/ratings/**").permitAll()

                        // Anything else falls back to public in this MVP
                        .anyRequest().permitAll()
                )
                // Use custom handler when authentication is required but missing/invalid.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                // Run our JWT filter before Spring's username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


}
