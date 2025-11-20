package com.microflix.rating_service.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.UUID;

// Represents the authenticated user extracted from the JWT
public record CurrentUser(
        UUID id,
        String email,
        List<String> roles
) {
}
