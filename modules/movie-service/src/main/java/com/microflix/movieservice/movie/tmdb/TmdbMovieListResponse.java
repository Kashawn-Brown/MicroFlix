package com.microflix.movieservice.movie.tmdb;

import java.util.List;

// Represents the whole response from TMDb movie lists (results -> list of movies)
public record TmdbMovieListResponse(
        int page,
        List<TmdbMovieResult> results
        ) {}
