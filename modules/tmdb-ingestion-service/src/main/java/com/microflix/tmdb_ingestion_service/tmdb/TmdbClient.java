package com.microflix.tmdb_ingestion_service.tmdb;

import com.microflix.tmdb_ingestion_service.movie.dto.TmdbMovieDetailResponse;
import com.microflix.tmdb_ingestion_service.tmdb.dto.TmdbMovieListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Thin wrapper around the TMDb API.
 * Responsible for making HTTP calls and mapping responses to DTOs.
 */
@Component
public class TmdbClient {

    // Logging for easier debugging.
    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public TmdbClient(
            @Value("${tmdb.base-url}") String baseUrl,
            @Value("${tmdb.api-key}") String apiKey,
            @Value("${tmdb.rate-limit.min-interval-ms:200}") long minIntervalMs,
            @Value("${tmdb.rate-limit.max-retries:3}") int maxRetries,
            @Value("${tmdb.rate-limit.retry-backoff-ms:2000}") long retryBackoffMs
    ) {

        // TMDb API key injected from configuration
        this.apiKey = apiKey;

        // Throttle TMDb traffic and retry on 429. Scoped to this client only —
        // MovieServiceClient's RestClient is intentionally unthrottled.
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor(throttlingInterceptor(minIntervalMs, maxRetries, retryBackoffMs))
                .build();
    }

    private ClientHttpRequestInterceptor throttlingInterceptor(
            long minIntervalMs, int maxRetries, long retryBackoffMs
    ) {
        return (request, body, execution) -> {
            sleepQuietly(minIntervalMs);

            int attempts = 0;
            while (true) {
                ClientHttpResponse response = execution.execute(request, body);
                if (response.getStatusCode().value() != 429 || attempts >= maxRetries) {
                    return response;
                }
                attempts++;
                log.warn("TMDb returned 429 (attempt {}/{}), backing off {}ms",
                        attempts, maxRetries, retryBackoffMs);
                response.close();
                sleepQuietly(retryBackoffMs);
            }
        };
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Fetches popular movies from Tmdb.
     */
    public TmdbMovieListResponse fetchPopularMovies(int page) {
        log.info("Fetching popular movies from TMDb, page={}", page);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/popular")
                        .queryParam("api_key", apiKey)      // TMDb v3 auth
                        .queryParam("page", page)
                        .queryParam("language", "en-US")
                        .build()
                )
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    /**
     * Fetches top-rated movies from TMDb.
     */
    public TmdbMovieListResponse fetchTopRatedMovies(int page) {
        log.info("Fetching top rated movies from TMDb, page={}", page);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/top_rated")
                        .queryParam("api_key", apiKey)      // TMDb v3 auth
                        .queryParam("page", page)
                        .queryParam("language", "en-US")
                        .build()
                )
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    /**
     * Fetches upcoming movies from TMDb.
     */
    public TmdbMovieListResponse fetchUpcomingMovies(int page) {
        log.info("Fetching upcoming movies from TMDb, page={}", page);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/upcoming")
                        .queryParam("api_key", apiKey)      // TMDb v3 auth
                        .queryParam("page", page)
                        .queryParam("language", "en-US")
                        .build()
                )
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    /**
     * Fetches now-playing movies from TMDb.
     */
    public TmdbMovieListResponse fetchNowPlayingMovies(int page) {
        log.info("Fetching now playing movies from TMDb, page={}", page);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/now_playing")
                        .queryParam("api_key", apiKey)      // TMDb v3 auth
                        .queryParam("page", page)
                        .queryParam("language", "en-US")
                        .build()
                )
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    /**
     * Fetches movies using the discover endpoint.
     */
    public TmdbMovieListResponse discoverPopularMovies(int page) {
        log.info("Discovering movies from TMDb, page={}", page);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("vote_count.gte", 250)
                        .queryParam("page", page)
                        .queryParam("language", "en-US")
                        .build()
                )
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }


    /**
     * Fetches recently-released movies via /discover. Used by scheduled ingestion
     * runs to surface new releases without paying for evergreen catalog pages.
     * sinceDate maps to TMDb's release_date.gte filter (inclusive). No vote_count
     * floor here — fresh releases haven't accumulated votes yet, and gating on
     * them would defeat the freshness goal.
     */
    public TmdbMovieListResponse discoverRecentMovies(int page, LocalDate sinceDate) {
        String sinceParam = sinceDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        log.info("Discovering recent movies from TMDb (release_date.gte={}), page={}", sinceParam, page);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("release_date.gte", sinceParam)
                        .queryParam("page", page)
                        .queryParam("language", "en-US")
                        .build()
                )
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }


    /**
     * Fetch a single movie's detailed info from TMDb by its tmdbId.
     */
    public TmdbMovieDetailResponse fetchMovieDetail(Long tmdbId) {
        log.info("Fetching TMDb movie details for tmdbId={}", tmdbId);

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("api_key", apiKey)
                        .build(tmdbId)
                )
                .retrieve()
                .body(TmdbMovieDetailResponse.class);
    }

}
