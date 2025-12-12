package com.microflix.tmdb_ingestion_service.movie.dto;

public record MovieSummary(
        Long id,
        Long tmdbId,
        String title
) {}
