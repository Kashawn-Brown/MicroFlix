package com.microflix.rating_service.rating.dto;

import java.util.UUID;

public record CreateRating(
        Long movieId,
        double rate
        ) {}
