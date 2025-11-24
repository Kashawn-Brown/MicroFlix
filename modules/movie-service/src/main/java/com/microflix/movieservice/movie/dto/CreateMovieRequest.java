package com.microflix.movieservice.movie.dto;


import java.util.List;

// Request payload for creating a new movie.
public record CreateMovieRequest(
        String title,
        String overview,
        Integer releaseYear,
        Integer runtime,
        Long tmdbId,
        List<String> genres
        ) {}
