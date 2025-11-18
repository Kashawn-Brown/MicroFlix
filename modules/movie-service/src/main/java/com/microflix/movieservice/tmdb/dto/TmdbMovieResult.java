package com.microflix.movieservice.tmdb.dto;

// Represents a single movie entry from TMDb's movie list responses
public record TmdbMovieResult(
        Long id,
        String title,
        String overview,
        String release_date,
        Long runtime
        ) {}
