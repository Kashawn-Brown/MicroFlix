package com.microflix.rating_service.rating.dto;

// Summary stats for all ratings on a movie.
public record MovieRatingSummaryResponse(
        Long movieId,
        Double average,
        Long count
) {
}
