package com.microflix.movieservice.movie.dto;

import java.util.List;

public record UpdateMovieRequest(
        String title,
        String overview,
        Integer releaseYear,
        Integer runtime,
        String posterUrl,
        String backdropUrl,
        List<String> genres
) {}
