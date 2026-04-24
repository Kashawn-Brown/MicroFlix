package com.microflix.movieservice.movie;

import com.microflix.movieservice.movie.dto.CreateMovieRequest;
import com.microflix.movieservice.movie.dto.GenreResponse;
import com.microflix.movieservice.movie.dto.MovieResponse;
import com.microflix.movieservice.movie.dto.UpdateMovieRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")       // Handles HTTP requests related to movie resources.
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

//    /**
//     * Returns all movies.
//     */
//    @GetMapping
//    public ResponseEntity<List<MovieResponse>> getAllMovies() {
//
//        var response = movieService.getAllMovies();
//
//        return ResponseEntity.ok(response);
//    }

    /**
     * Returns a single movie by its id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovie(@PathVariable Long id) {

        var response = movieService.getMovie(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns movies matching the given ids, in input-id order.
     *
     *   GET /api/v1/movies/batch?ids=12,7,42
     *
     * Unknown ids are silently dropped. Capped at {@link MovieService#MAX_BATCH_SIZE} ids
     * per call. Used by the gateway's watchlist aggregation endpoint to hydrate engagement
     * rows in a single round-trip instead of fanning out N /{id} calls.
     */
    @GetMapping("/batch")
    public ResponseEntity<List<MovieResponse>> getMoviesByIds(@RequestParam List<Long> ids) {

        var response = movieService.getMoviesByIds(ids);

        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new movie and returns it with HTTP 201.
     *
     * Now implemented in Internal Controller (only to be used by Tmdb ingestion service)
     */
    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@RequestBody CreateMovieRequest request) {


        var response = movieService.createMovie(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Partially updates a single movie by its id.
     *
     * Now implemented in Internal Controller (only to be used by Tmdb ingestion service)
     */
    @PatchMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(@PathVariable Long id, @RequestBody UpdateMovieRequest request) {

        var response = movieService.updateMovie(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * List / search movies with optional filters:
     *  - query: text search on title (contains, case-insensitive)
     *  - genre: exact genre name (case-insensitive), e.g. "Action"
     *  - year: release year, e.g. 2010
     *  - sort: sort key, e.g. "created_desc" (default), "title_asc", "year_desc"
     *
     * Examples:
     *  GET /api/v1/movies
     *  GET /api/v1/movies?query=inception
     *  GET /api/v1/movies?genre=Action&year=2010
     *  GET /api/v1/movies?sort=title_asc
     */
    @GetMapping
    public ResponseEntity<Page<MovieResponse>> searchMovies(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "created_desc") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {

        var response = movieService.searchMovies(query, genre, year, sort, page, size);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/genres")
    public ResponseEntity<List<GenreResponse>> listGenres() {

        var response = movieService.listGenres();

        return ResponseEntity.ok(response);

    }
}
