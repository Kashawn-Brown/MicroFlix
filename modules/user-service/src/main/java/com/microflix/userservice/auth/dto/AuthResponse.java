package com.microflix.userservice.auth.dto;

public record AuthResponse(
        String token,
        String email,
        String displayName,
        String roles
        ) {}
