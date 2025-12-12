package com.microflix.tmdb_ingestion_service.movie.dto;

public record TmdbMovieDetailResponse(
        Long id,
        String title,
        String overview,
        Integer runtime,
        String release_date,
        String poster_path,
        String backdrop_path
) {}
