package com.microflix.movieservice.movie.dto;

public record CreateMovieRequest(
        String title,
        String overview,
        Integer releaseYear,
        Integer runtime,
        Long tmdbId
        ) {}
