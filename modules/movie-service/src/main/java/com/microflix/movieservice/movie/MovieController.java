package com.microflix.movieservice.movie;

import com.microflix.movieservice.movie.dto.CreateMovieRequest;
import com.microflix.movieservice.movie.dto.MovieResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {

        var response = movieService.getAllMovies();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovie(@PathVariable Long id) {

        var response = movieService.getMovie(id);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@RequestBody CreateMovieRequest request) {


        var response = movieService.createMovie(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
