package com.microflix.movieservice.movie.dto;

public record MovieSummaryResponse(
        Long id,
        Long tmdbId,
        String title
) {}
