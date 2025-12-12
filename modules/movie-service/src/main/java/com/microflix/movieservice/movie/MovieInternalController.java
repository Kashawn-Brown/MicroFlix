package com.microflix.movieservice.movie;

import com.microflix.movieservice.movie.dto.CreateMovieRequest;
import com.microflix.movieservice.movie.dto.MovieResponse;
import com.microflix.movieservice.movie.dto.MovieSummaryResponse;
import com.microflix.movieservice.movie.dto.UpdateMovieRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal-only endpoints used by background jobs / other internal services.
 * Not meant for public clients.
 */
@RestController
@RequestMapping("/api/internal/v1/movies")
public class MovieInternalController {

    private final MovieRepository movieRepository;
    private final MovieService movieService;

    public MovieInternalController(MovieRepository movieRepository, MovieService movieService) {
        this.movieRepository = movieRepository;
        this.movieService = movieService;
    }

    /**
     * Creates a new movie and returns HTTP 201.
     */
    @PostMapping
    public ResponseEntity<Void> createMovie(@RequestBody CreateMovieRequest request) {

        movieService.createMovie(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Partially updates a single movie by its id.
     */
    @PatchMapping("/{movieId}")
    public ResponseEntity<Void> updateMovie(@PathVariable Long movieId, @RequestBody UpdateMovieRequest request) {

        movieService.updateMovie(movieId, request);

        return ResponseEntity.noContent().build();
    }

    /**
     * Partially updates a single movie by its Tmdb id.
     */
    @PatchMapping("/by-tmdb/{tmdbId}")
    public ResponseEntity<Void> updateMovieTmdb(@PathVariable Long tmdbId, @RequestBody UpdateMovieRequest request) {

        movieService.updateMovieTmdb(tmdbId, request);

        return ResponseEntity.noContent().build();
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

    /**
     * Returns movies that still need a runtime from TMDb.
     */
    @GetMapping("/needs-runtime")
    public ResponseEntity<List<MovieSummaryResponse>> getMoviesNeedingRuntime(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        var response = movieService.findMoviesNeedingRuntime(pageable);

        return ResponseEntity.ok(response.getContent());    // getContent to return a list
    }


}
