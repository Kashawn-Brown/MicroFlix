package com.microflix.movieservice.movie.tmdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TmdbClient {

    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public TmdbClient(@Value("${tmdb.base-url}") String baseUrl, @Value("${tmdb.api-key}") String apiKey) {

        // Set up Tmdb API key
        this.apiKey = apiKey;

        // Build a RestClient configured with base URL
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

    }

    /**
     * Fetches the first page of movies from TMDb.
     * For now only supporting page=1 to keep seeding simple.
     */
    public TmdbMovieListResponse fetchPopularMovies() {
        log.info("Fetching popular movies from TMDb");

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/popular")
                        .queryParam("api_key", apiKey)      // TMDb v3 auth
                        .queryParam("page", 1)
                        .queryParam("language", "en-US")
                        .build()
                )
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    public TmdbMovieListResponse fetchTopRatedMovies() {
        log.info("Fetching top rated movies from TMDb");

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/top_rated")
                        .queryParam("api_key", apiKey)      // TMDb v3 auth
                        .queryParam("page", 1)
                        .queryParam("language", "en-US")
                        .build()
                )
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    public TmdbMovieListResponse fetchUpcomingMovies() {
        log.info("Fetching upcoming movies from TMDb");

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/upcoming")
                        .queryParam("api_key", apiKey)      // TMDb v3 auth
                        .queryParam("page", 1)
                        .queryParam("language", "en-US")
                        .build()
                )
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }

    public TmdbMovieListResponse fetchNowPlayingMovies() {
        log.info("Fetching now playing movies from TMDb");

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/now_playing")
                        .queryParam("api_key", apiKey)      // TMDb v3 auth
                        .queryParam("page", 1)
                        .queryParam("language", "en-US")
                        .build()
                )
                .retrieve()
                .body(TmdbMovieListResponse.class);
    }



}
