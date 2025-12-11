/// REDUNDANT
/// NOW REPLACED WITH PROPER INGESTION SERVICE


//package com.microflix.movieservice.tmdb;
//
//import com.microflix.movieservice.tmdb.dto.TmdbMovieListResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestClient;
//
///**
// * Thin wrapper around the TMDb API.
// * Responsible for making HTTP calls and mapping responses to DTOs.
// */
//@Component
//public class TmdbClient {
//
//    // Logging for easier debugging.
//    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);
//
//    private final RestClient restClient;
//    private final String apiKey;
//
//    public TmdbClient(@Value("${tmdb.base-url}") String baseUrl, @Value("${tmdb.api-key}") String apiKey) {
//
//        // TMDb API key injected from configuration
//        this.apiKey = apiKey;
//
//        // Pre-configured RestClient with the TMDb base URL (avoid repeating it on every call)
//        this.restClient = RestClient.builder()
//                .baseUrl(baseUrl)
//                .build();
//
//    }
//
//    /**
//     * Fetches the first page of movies from TMDb.
//     * For now only supporting page=1 to keep seeding simple.
//     */
//    public TmdbMovieListResponse fetchPopularMovies() {
//        log.info("Fetching popular movies from TMDb");
//
//        return restClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/movie/popular")
//                        .queryParam("api_key", apiKey)      // TMDb v3 auth
//                        .queryParam("page", 1)
//                        .queryParam("language", "en-US")
//                        .build()
//                )
//                .retrieve()
//                .body(TmdbMovieListResponse.class);
//    }
//
//    /**
//     * Fetches the first page of top-rated movies from TMDb.
//     */
//    public TmdbMovieListResponse fetchTopRatedMovies() {
//        log.info("Fetching top rated movies from TMDb");
//
//        return restClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/movie/top_rated")
//                        .queryParam("api_key", apiKey)      // TMDb v3 auth
//                        .queryParam("page", 1)
//                        .queryParam("language", "en-US")
//                        .build()
//                )
//                .retrieve()
//                .body(TmdbMovieListResponse.class);
//    }
//
//    /**
//     * Fetches the first page of upcoming movies from TMDb.
//     */
//    public TmdbMovieListResponse fetchUpcomingMovies() {
//        log.info("Fetching upcoming movies from TMDb");
//
//        return restClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/movie/upcoming")
//                        .queryParam("api_key", apiKey)      // TMDb v3 auth
//                        .queryParam("page", 1)
//                        .queryParam("language", "en-US")
//                        .build()
//                )
//                .retrieve()
//                .body(TmdbMovieListResponse.class);
//    }
//
//    /**
//     * Fetches the first page of now-playing movies from TMDb.
//     */
//    public TmdbMovieListResponse fetchNowPlayingMovies() {
//        log.info("Fetching now playing movies from TMDb");
//
//        return restClient.get()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/movie/now_playing")
//                        .queryParam("api_key", apiKey)      // TMDb v3 auth
//                        .queryParam("page", 1)
//                        .queryParam("language", "en-US")
//                        .build()
//                )
//                .retrieve()
//                .body(TmdbMovieListResponse.class);
//    }
//
//
//
//}
