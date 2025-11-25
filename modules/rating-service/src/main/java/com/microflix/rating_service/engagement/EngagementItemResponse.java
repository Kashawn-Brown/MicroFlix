package com.microflix.rating_service.engagement;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One item in the user's list.
 */
public record EngagementItemResponse(
        UUID userId,
        Long movieId,
        EngagementType type,
        OffsetDateTime addedAt
        ) {}
