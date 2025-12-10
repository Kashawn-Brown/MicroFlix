package com.microflix.gateway.catalog.dto;

///  Response to give everything the movie detail page needs in one hit
/// The movies info, rating summary, and user info
public record CatalogMovieDetailsResponse(
        CatalogMovieDto movie,
        CatalogRatingSummaryDto ratingSummary,
        CatalogMeDto me
) {}
