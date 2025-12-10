package com.microflix.gateway.catalog;

import com.microflix.gateway.catalog.dto.*;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

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
}
