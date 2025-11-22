package com.microflix.userservice.user.dto;

import java.util.List;
import java.util.UUID;

// API response representing a user.
public record ProfileMeResponse(
        UUID id,
        String email,
        String displayName,
        List<String> roles
) {}
