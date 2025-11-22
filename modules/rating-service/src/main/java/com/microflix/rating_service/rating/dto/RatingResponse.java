package com.microflix.rating_service.rating.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// API response representing a single rating.
public record RatingResponse(
        Long id,
        UUID userId,
        Long movieId,
        double rate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
        ) {}
