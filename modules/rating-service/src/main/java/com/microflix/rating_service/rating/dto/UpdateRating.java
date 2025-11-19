package com.microflix.rating_service.rating.dto;

import java.util.UUID;

public record UpdateRating(
        UUID userId,
        Long movieId,
        double rate
        ) {}
