package com.microflix.gateway.catalog.dto;

import java.time.OffsetDateTime;
import java.util.List;

///  Movie data used on the detail page, coming from movie-service
///  Includes core fields plus genres and picture paths
public record CatalogMovieDto(
        Long id,
        String title,
        String overview,
        Integer releaseYear,
        Integer runtime,
        String posterUrl,
        String backdropUrl,
        List<String> genres
) {}
