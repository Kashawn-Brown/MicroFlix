package com.microflix.gateway.catalog.dto;

import java.time.OffsetDateTime;

/**
 * One row on the aggregated watchlist response: the movie metadata hydrated
 * from movie-service, paired with the addedAt timestamp from the rating-service
 * engagement record. The response is a flat array of these in addedAt-desc order.
 */
public record CatalogWatchlistItemDto(
        CatalogMovieDto movie,
        OffsetDateTime addedAt
) {}
