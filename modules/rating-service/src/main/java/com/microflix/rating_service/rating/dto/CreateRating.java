package com.microflix.rating_service.rating.dto;

// Request body for creating a rating for a movie.
public record CreateRating(
        Long movieId,
        double rate
        ) {}
