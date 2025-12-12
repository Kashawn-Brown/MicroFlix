package com.microflix.tmdb_ingestion_service.movie.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record UpdateMovieRequest(
        String title,
        String overview,
        Integer releaseYear,
        Integer runtime,
        String posterUrl,
        String backdropUrl,
        List<String> genres
) {}
