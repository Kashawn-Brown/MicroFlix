package com.microflix.gateway.catalog.dto;

///  Current user's rating on this movie
///  Their rating (if any) and whether it's in their watchlist
public record CatalogMeDto(
        Double rating,     // my rating 1.0–10.0, or null
        boolean inWatchlist
) {
    // Helper for when user not logged in
    public static CatalogMeDto anonymous() {
        return new CatalogMeDto(null, false);
    }
}
