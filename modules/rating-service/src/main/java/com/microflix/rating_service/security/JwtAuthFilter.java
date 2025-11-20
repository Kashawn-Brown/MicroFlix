package com.microflix.rating_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Runs once per request, looks for a Bearer token, and if valid,
// sets the CurrentUser into Spring Security's context.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtVerifier jwtVerifier;

    public JwtAuthFilter(JwtVerifier jwtVerifier) {
        this.jwtVerifier = jwtVerifier;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {// Retrieving Authorization header
            String header = request.getHeader("Authorization");

            // If token present
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {

                // Strip "Bearer " prefix
                String token = header.substring(7);

                // Convert the JWT into CurrentUser model
                CurrentUser currentUser = jwtVerifier.verify(token);

                // Map roles ("USER") to Spring authorities ("ROLE_USER")
                List<SimpleGrantedAuthority> authorities = currentUser.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList();

                // Build an authentication object with CurrentUser as the principal
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(currentUser, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // For now log and continue as anonymous.
            // SecurityConfig will decide if anonymous is allowed for the endpoint.
            log.warn("Failed to authenticate JWT: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        // Always continue filter chain
        filterChain.doFilter(request, response);
    }

}