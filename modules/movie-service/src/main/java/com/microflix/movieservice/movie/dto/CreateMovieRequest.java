package com.microflix.movieservice.movie.dto;


// Request payload for creating a new movie.
public record CreateMovieRequest(
        String title,
        String overview,
        Integer releaseYear,
        Integer runtime,
        Long tmdbId
        ) {}
