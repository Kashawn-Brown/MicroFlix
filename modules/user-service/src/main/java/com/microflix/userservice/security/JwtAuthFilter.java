package com.microflix.userservice.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.microflix.userservice.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT Filter
 * Runs once per request before controllers.
 * Goal: if the request has a valid "Authorization: Bearer <JWT>" header,
 *       put an authenticated user (email + roles) into Spring Security's context.
 * If no/invalid token -> do nothing here; downstream rules may reject later.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    // This method is called automatically for every incoming request.
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        try {

            // Retrieving Authorization header
            String header = request.getHeader("Authorization");

            // If token present
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                DecodedJWT decoded = jwt.verifyToken(token);

                String email = decoded.getSubject();
                String rolesCsv = decoded.getClaim("roles").asString();

                // Map users roles -> Spring authorities (must be "ROLE_X")
                var authorities = Arrays.stream(rolesCsv.split(","))
                        .filter(StringUtils::hasText)                                   // drop blanks/empty items
                        .map(String::trim)                                              // remove leading/trailing spaces
                        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                // Setting authenticated user info for the current request thread
                var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch(Exception e) {
            log.warn("Failed to authenticate JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // Continue the filter chain - Passes request to the next filter/controller
        chain.doFilter(request, response);


    }
}
