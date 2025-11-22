package com.microflix.movieservice.common.errors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * Maps movie-related exceptions to HTTP error responses.
 */
@RestControllerAdvice
public class MovieErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(MovieErrorAdvice.class);

    // Handles "movie not found" errors as HTTP 404 with a ProblemDetail body.
    @ExceptionHandler(MovieNotFoundException.class)
    ProblemDetail handleMovieNotFound(MovieNotFoundException ex) {

        log.info("Movie not found: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Movie not found");

        return problem;
    }
}
