package com.microflix.tmdb_ingestion_service.movie.dto;

import java.util.List;

/**
 * Mirrors the CreateMovieRequest used by movie-service.
 * As long as the fields and names match, JSON serialization will line up.
 */
public record CreateMovieRequest(
        String title,
        String overview,
        Integer releaseYear,
        Integer runtime,
        Long tmdbId,
        String posterUrl,
        String backdropUrl,
        List<String> genres
) {}
