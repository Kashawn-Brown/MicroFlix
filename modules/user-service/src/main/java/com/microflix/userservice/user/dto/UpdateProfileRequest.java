package com.microflix.userservice.user.dto;

import jakarta.validation.constraints.Size;

// Request body for updating a user (only update to displayName  for now).
public record UpdateProfileRequest(
        @Size(min = 1, max = 100, message = "display name must be 1-100 chars") String displayName
        ) {}
