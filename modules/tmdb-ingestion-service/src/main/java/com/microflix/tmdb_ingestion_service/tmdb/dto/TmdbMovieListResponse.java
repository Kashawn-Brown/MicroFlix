package com.microflix.tmdb_ingestion_service.tmdb.dto;

import java.util.List;

// Represents the whole response from TMDb movie lists (results -> list of movies)
public record TmdbMovieListResponse(
        int page,
        List<TmdbMovieResult> results
        ) {}
