package com.microflix.movieservice.movie;

import com.microflix.movieservice.movie.dto.CreateMovieRequest;
import com.microflix.movieservice.movie.dto.MovieResponse;
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
     * Creates a new movie and returns it with HTTP 201.
     */
    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@RequestBody CreateMovieRequest request) {


        var response = movieService.createMovie(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
}
