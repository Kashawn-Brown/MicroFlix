package com.microflix.movieservice.movie;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal-only endpoints used by background jobs / other internal services.
 * Not meant for public clients.
 */
@RestController
@RequestMapping("/api/internal/v1/movies")
public class MovieInternalController {

    private final MovieRepository movieRepository;

    public MovieInternalController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * Returns true if a movie already exists with the given TMDb id.
     * This lets ingestion jobs be idempotent and avoid duplicates.
     */
    @GetMapping("/exists-by-tmdb/{tmdbId}")
    public ResponseEntity<Boolean> existsByTmdbId(@PathVariable Long tmdbId) {
        boolean exists = movieRepository.existsByTmdbId(tmdbId);
        return ResponseEntity.ok(exists);
    }
}
