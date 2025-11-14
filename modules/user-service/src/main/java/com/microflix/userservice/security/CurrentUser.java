package com.microflix.userservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

/**
 * Represents the currently authenticated user in our system.
 * Created so services don't need to know about Authentication.
 */
public record CurrentUser(
        String email,
        List<String> roles
) {

    /**
     * to build a CurrentUser from a Spring Security Authentication
     */
    public static CurrentUser set(Authentication auth) {
        var roles = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        return new CurrentUser(auth.getName(), roles);
    }
}
