package com.microflix.userservice.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Incoming JSON for users to login */
public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8, max = 72) String password
        ) {}
