package com.microflix.userservice.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 1, max = 100, message = "display name must be 1-100 chars") String displayName
        ) {}
