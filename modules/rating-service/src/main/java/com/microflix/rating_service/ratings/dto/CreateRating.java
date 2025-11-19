package com.microflix.rating_service.ratings.dto;

import java.util.UUID;

public record CreateRating(
        // TODO: once auth is wired, we'll set userId from the authenticated user
        UUID userId,                    // Will be removed
        Long movieId,
        double rate
        ) {}
