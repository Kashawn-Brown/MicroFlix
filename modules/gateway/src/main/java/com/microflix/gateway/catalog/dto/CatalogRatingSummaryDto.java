package com.microflix.gateway.catalog.dto;

///  Aggregated rating summary from rating-service
///  Average score for the movie and how many ratings contributed
public record CatalogRatingSummaryDto(
        Double average,   // e.g. 8.4
        Long count        // e.g. 123 ratings
) {
    public static CatalogRatingSummaryDto empty() {
        return new CatalogRatingSummaryDto(null, 0L);
    }
}
