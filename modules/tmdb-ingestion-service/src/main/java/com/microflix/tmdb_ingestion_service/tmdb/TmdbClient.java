package com.microflix.tmdb_ingestion_service.tmdb;

import com.microflix.tmdb_ingestion_service.movie.dto.TmdbMovieDetailResponse;
import com.microflix.tmdb_ingestion_service.tmdb.dto.TmdbMovieListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public TmdbClient(@Value("${tmdb.base-url}") String baseUrl, @Value("${tmdb.api-key}") String apiKey) {

        // TMDb API key injected from configuration
        this.apiKey = apiKey;

        // Pre-configured RestClient with the TMDb base URL (avoid repeating it on every call).
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
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
