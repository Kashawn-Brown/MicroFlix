package com.microflix.gateway.catalog;

import com.microflix.gateway.catalog.dto.CatalogMovieDto;
import com.microflix.gateway.catalog.dto.CatalogWatchlistItemDto;
import com.microflix.gateway.catalog.dto.EngagementDto;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the pure-function join that glues watchlist engagements to movie metadata.
 *
 * This is the only piece of the watchlist aggregation with non-trivial logic — the rest
 * is WebClient wiring that would need a MockWebServer / WireMock to exercise. Rather than
 * stand that up for one endpoint (no existing pattern in the gateway module), we test the
 * correctness-critical helper directly and rely on the live stack for end-to-end coverage
 * via the Piece 5 k6 measurement run.
 */
class CatalogServiceWatchlistJoinTest {

    private static final OffsetDateTime T0 = OffsetDateTime.of(2026, 4, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime T1 = T0.plusHours(1);
    private static final OffsetDateTime T2 = T0.plusHours(2);

    @Test
    void joinWatchlist_preservesEngagementOrder_evenWhenBatchReturnsOutOfOrder() {
        // rating-service returns engagements newest-first (addedAt desc): movie 3, 1, 2.
        List<EngagementDto> engagements = List.of(
                new EngagementDto(3L, T2),
                new EngagementDto(1L, T1),
                new EngagementDto(2L, T0)
        );
        // movie-service returns the hydrated movies in a deliberately different order —
        // proves the helper rebuilds engagement order, not DB order.
        List<CatalogMovieDto> movies = List.of(
                movie(1L, "Inception"),
                movie(2L, "Interstellar"),
                movie(3L, "Tenet")
        );

        List<CatalogWatchlistItemDto> result = CatalogService.joinWatchlist(engagements, movies);

        assertEquals(List.of(3L, 1L, 2L), result.stream().map(i -> i.movie().id()).toList());
        assertEquals(T2, result.get(0).addedAt());
        assertEquals("Tenet", result.get(0).movie().title());
        assertEquals(T1, result.get(1).addedAt());
        assertEquals(T0, result.get(2).addedAt());
    }

    @Test
    void joinWatchlist_dropsEngagementsWhoseMovieWentMissing() {
        // Engagement for id 2 is stale — movie-service couldn't hydrate it.
        List<EngagementDto> engagements = List.of(
                new EngagementDto(1L, T1),
                new EngagementDto(2L, T0)
        );
        List<CatalogMovieDto> movies = List.of(
                movie(1L, "Inception")
        );

        List<CatalogWatchlistItemDto> result = CatalogService.joinWatchlist(engagements, movies);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).movie().id());
        assertEquals(T1, result.get(0).addedAt());
    }

    @Test
    void joinWatchlist_emptyEngagements_returnsEmptyList() {
        List<CatalogWatchlistItemDto> result =
                CatalogService.joinWatchlist(List.of(), List.of(movie(1L, "Inception")));

        assertTrue(result.isEmpty());
    }

    @Test
    void joinWatchlist_emptyMovies_dropsEverything() {
        List<CatalogWatchlistItemDto> result = CatalogService.joinWatchlist(
                List.of(new EngagementDto(1L, T1), new EngagementDto(2L, T0)),
                List.of()
        );

        assertTrue(result.isEmpty());
    }

    private static CatalogMovieDto movie(Long id, String title) {
        return new CatalogMovieDto(id, title, null, null, null, null, null, List.of());
    }
}
