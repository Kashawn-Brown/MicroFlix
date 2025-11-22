package com.microflix.rating_service.rating.dto;

// Request body for updating an existing rating for a movie.
public record UpdateRating(
        Long movieId,
        double rate
        ) {}
