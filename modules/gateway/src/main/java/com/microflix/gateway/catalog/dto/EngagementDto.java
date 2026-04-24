package com.microflix.gateway.catalog.dto;

import java.time.OffsetDateTime;

/**
 * Narrow internal view of rating-service's EngagementItemResponse — we only need
 * movieId + addedAt for watchlist hydration, and Jackson ignores the unused fields
 * (Spring Boot default: fail-on-unknown-properties = false).
 */
public record EngagementDto(
        Long movieId,
        OffsetDateTime addedAt
) {}
