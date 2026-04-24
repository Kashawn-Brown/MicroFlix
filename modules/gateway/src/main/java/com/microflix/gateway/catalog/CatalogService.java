package com.microflix.gateway.catalog;

import com.microflix.gateway.catalog.dto.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Coordinates calls to downstream microservices to build aggregated catalog responses for the gateway.
 *
 * - Call movie-service for movie metadata
 * - Call rating-service for rating summary + "me" (rating + watchlist)
 * - Combine everything into a single DTO for the controller
 */
@Service
public class CatalogService {

    private final WebClient.Builder webClientBuilder;

    public CatalogService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Build the aggregated movie details response.
     */
    public Mono<CatalogMovieDetailsResponse> getMovieDetails(Long movieId, String authHeader) {
        WebClient client = webClientBuilder.build();

        Mono<CatalogMovieDto> movieMono = fetchMovie(client, movieId);
        Mono<CatalogRatingSummaryDto> summaryMono = fetchRatingSummary(client, movieId);
        Mono<CatalogMeDto> meMono = fetchMeSection(client, movieId, authHeader);

        // Wait for all three calls to complete, then assemble the response.
        return Mono.zip(movieMono, summaryMono, meMono)
                .map(tuple -> new CatalogMovieDetailsResponse(
                        tuple.getT1(),  // movie
                        tuple.getT2(),  // ratingSummary
                        tuple.getT3()   // me
                ));
    }

    /**
     * Fetch movie metadata from movie-service.
     */
    private Mono<CatalogMovieDto> fetchMovie(WebClient client, Long movieId) {
        return client.get()
                .uri("lb://movie-service/api/v1/movies/{id}", movieId)
                .retrieve()
                .bodyToMono(CatalogMovieDto.class);
        // If movie doesn't exist, this will error; you can optionally map 404 -> ProblemDetail later.
    }

    /**
     * Fetch rating summary (average + count) from rating-service.
     * If no ratings exist yet, treat 404 as "empty summary".
     */
    private Mono<CatalogRatingSummaryDto> fetchRatingSummary(WebClient client, Long movieId) {
        return client.get()
                .uri("lb://rating-service/api/v1/ratings/movie/{id}/summary", movieId)
                .retrieve()
                .bodyToMono(CatalogRatingSummaryDto.class)
                .onErrorResume(WebClientResponseException.NotFound.class,
                        ex -> Mono.just(CatalogRatingSummaryDto.empty())
                );
    }

    /**
     * Build the "me" section (my rating + watchlist status).
     * If there is no Authorization header, returns an anonymous view.
     */
    private Mono<CatalogMeDto> fetchMeSection(WebClient client, Long movieId, String authHeader) {

        // Not logged in → anonymous view (no rating, not in watchlist)
        if (authHeader == null || authHeader.isBlank()) {
            return Mono.just(CatalogMeDto.anonymous());
        }

        Mono<Double> myRatingMono = client.get()
                .uri("lb://rating-service/api/v1/ratings/movie/{id}/me", movieId)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(MyRatingDto.class)
                .map(MyRatingDto::rate)       // map from rating-service DTO to just the score
                .onErrorResume(WebClientResponseException.NotFound.class,
                        ex -> Mono.just(null)   // no rating yet
                );

        Mono<Boolean> inWatchlistMono = client.get()
                .uri("lb://rating-service/api/v1/engagements/watchlist/{id}/me", movieId)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(WebClientResponseException.NotFound.class,
                        ex -> Mono.just(false)  // not in watchlist
                );

        // Combine my rating + watchlist flag into a single "me" DTO
        return Mono.zip(myRatingMono, inWatchlistMono)
                .map(tuple -> new CatalogMeDto(
                        tuple.getT1(),    // rating (may be null)
                        tuple.getT2()     // inWatchlist
                ));
    }

    /**
     * Build the aggregated watchlist response.
     *
     * Two downstream calls, sequenced: rating-service for engagements, then movie-service's
     * batch endpoint to hydrate those engagements in one query instead of fanning out N
     * /{id} calls. Empty watchlist short-circuits without touching movie-service.
     *
     * Order preserved throughout: rating-service returns engagements in addedAt-desc order,
     * and the join helper below rebuilds the response list in that same order — since the
     * movie-service batch endpoint also preserves input-id order, we could rely on its
     * response directly, but doing the Map-based join here is robust to any reordering and
     * also drops engagements whose movie row has gone missing (stale engagement rows).
     */
    public Mono<List<CatalogWatchlistItemDto>> getWatchlist(String authHeader) {
        WebClient client = webClientBuilder.build();

        return fetchWatchlistEngagements(client, authHeader)
                .flatMap(engagements -> {
                    if (engagements.isEmpty()) {
                        return Mono.just(List.<CatalogWatchlistItemDto>of());
                    }
                    List<Long> movieIds = engagements.stream().map(EngagementDto::movieId).toList();
                    return fetchMoviesBatch(client, movieIds)
                            .map(movies -> joinWatchlist(engagements, movies));
                });
    }

    /**
     * Fetch the current user's engagement rows from rating-service. Authorization header
     * is required — rating-service resolves @AuthenticationPrincipal from it.
     */
    private Mono<List<EngagementDto>> fetchWatchlistEngagements(WebClient client, String authHeader) {
        return client.get()
                .uri("lb://rating-service/api/v1/engagements/watchlist")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<EngagementDto>>() {});
    }

    /**
     * Hydrate a list of movie ids via movie-service's batch endpoint in one round-trip.
     */
    private Mono<List<CatalogMovieDto>> fetchMoviesBatch(WebClient client, List<Long> movieIds) {
        String idsCsv = movieIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return client.get()
                .uri("lb://movie-service/api/v1/movies/batch?ids={ids}", idsCsv)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<CatalogMovieDto>>() {});
    }

    /**
     * Pure-function join: produce watchlist items in engagement order, dropping any whose
     * movie row no longer exists. Extracted for unit-testability — this is the only piece
     * of the watchlist aggregation that has non-trivial logic.
     */
    static List<CatalogWatchlistItemDto> joinWatchlist(
            List<EngagementDto> engagements,
            List<CatalogMovieDto> movies
    ) {
        Map<Long, CatalogMovieDto> byId = new HashMap<>(movies.size());
        for (CatalogMovieDto movie : movies) {
            byId.put(movie.id(), movie);
        }

        List<CatalogWatchlistItemDto> result = new ArrayList<>(engagements.size());
        for (EngagementDto engagement : engagements) {
            CatalogMovieDto movie = byId.get(engagement.movieId());
            if (movie != null) {
                result.add(new CatalogWatchlistItemDto(movie, engagement.addedAt()));
            }
        }
        return result;
    }
}
