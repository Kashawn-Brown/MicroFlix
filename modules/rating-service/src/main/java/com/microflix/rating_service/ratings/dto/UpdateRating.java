package com.microflix.rating_service.ratings.dto;

import java.util.UUID;

public record UpdateRating(
        UUID userId,
        Long movieId,
        double rate
        ) {}
