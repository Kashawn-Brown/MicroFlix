package com.microflix.userservice.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Protected sample endpoint:
 * - requires a valid JWT (per security rules)
 * - returns the current user's email + roles
 */
@RestController
public class ProfileController {

    // Spring injects Authentication from SecurityContext (set by our JwtAuthFilter)
    @GetMapping("/api/profile/me")
    public Map<String, Object> me(Authentication auth) {            // Authentication is a recognized parameter type -> reads the Authentication from SecurityContextHolder.getContext() and passes it in
        return Map.of(
                "email", auth.getName(),
                "roles", auth.getAuthorities().stream().map(Object::toString).toList()
        );
    }


}
