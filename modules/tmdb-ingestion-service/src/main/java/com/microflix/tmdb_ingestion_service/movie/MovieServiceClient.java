package com.microflix.tmdb_ingestion_service.movie;

import com.microflix.tmdb_ingestion_service.movie.dto.CreateMovieRequest;
import com.microflix.tmdb_ingestion_service.movie.dto.MovieSummary;
import com.microflix.tmdb_ingestion_service.movie.dto.UpdateMovieRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Small HTTP client for talking to movie-service.
 * Hides the exact URLs used to create movies and check for existing ones.
 */
@Component
public class MovieServiceClient {

    private static final Logger log = LoggerFactory.getLogger(MovieServiceClient.class);

    private final RestClient restClient;

    public MovieServiceClient(@Value("${movie-service.base-url}") String baseUrl) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Checks if a movie already exists in movie-service for a given TMDb id.
     * Uses the internal endpoint we added earlier.
     */
    public boolean existsByTmdbId(Long tmdbId) {
        Boolean body = restClient.get()
                .uri("/api/internal/v1/movies/exists-by-tmdb/{tmdbId}", tmdbId)
                .retrieve()
                .body(Boolean.class);

        return body != null && body;
    }

    /**
     * Calls the internal movie creation endpoint.
     * We rely on movie-service to handle DB writes and genre relationships.
     */
    public void createMovie(CreateMovieRequest request) {
        ResponseEntity<Void> response = restClient.post()
                .uri("/api/internal/v1/movies")
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.debug("Created movie '{}' (HTTP {})", request.title(), response.getStatusCode());
    }

    /**
     * Calls the internal movie update endpoint.
     */
    public void updateMovie(Long movieId, UpdateMovieRequest request) {
        ResponseEntity<Void> response = restClient.patch()
                .uri("/api/internal/v1/movies/{id}", movieId)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.debug("Updated movie '{}' (HTTP {})", request.title(), response.getStatusCode());
    }

    /**
     * Calls the internal movie update endpoint to update a movie using its TMDb id
     */
    public void updateMovieByTmdbId(Long tmdbId, UpdateMovieRequest request) {
        ResponseEntity<Void> response = restClient.patch()
                .uri("/api/internal/v1/movies/by-tmdb/{tmdbId}", tmdbId)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        log.debug("Updated movie by tmdbId={} '{}' (HTTP {})", tmdbId, request.title(), response.getStatusCode());
    }

    /**
     * Fetches movies from movie-service that still need runtime enrichment.
     */
    public List<MovieSummary> findMoviesNeedingRuntime(int page, int size) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/internal/v1/movies/needs-runtime")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build()
                )
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});   // will help infer List<MovieRuntimeSummary>
    }
}
