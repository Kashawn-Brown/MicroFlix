package com.microflix.movieservice.movie.dto;

import java.time.OffsetDateTime;

// Response DTO returned to clients for a single movie
public record MovieResponse(
        Long id,
        String title,
        String overview,
        Integer releaseYear,
        Integer runtime,
        Long tmdbId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
