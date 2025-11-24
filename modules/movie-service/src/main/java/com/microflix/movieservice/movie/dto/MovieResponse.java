package com.microflix.movieservice.movie.dto;

import java.time.OffsetDateTime;
import java.util.List;

// Response DTO returned to clients for a single movie
public record MovieResponse(
        Long id,
        String title,
        String overview,
        Integer releaseYear,
        Integer runtime,
        Long tmdbId,
        List<String> genres,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
