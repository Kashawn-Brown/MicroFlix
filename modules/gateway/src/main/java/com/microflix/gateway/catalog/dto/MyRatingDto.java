package com.microflix.gateway.catalog.dto;

///  Internal DTO for rating-service `/ratings/movie/{id}/me`
///  Only care about the rating value from that response
public record MyRatingDto(
        Double rate
) {}