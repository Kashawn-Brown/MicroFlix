package com.microflix.rating_service.rating.dto;

public record MovieRatingSummaryResponse(
        Long movieId,
        Double average,
        Long count
) {
}
