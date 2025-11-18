package com.microflix.movieservice.common.errors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * Central place for movie-related exceptions -> HTTP response mapping.
 */
@RestControllerAdvice
public class MovieErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(MovieErrorAdvice.class);

    @ExceptionHandler(MovieNotFoundException.class)
    ProblemDetail handleMovieNotFound(MovieNotFoundException ex) {

        // Log at debug/info level – this is a normal 404, not a server bug
        log.info("Movie not found: {}", ex.getMessage());

        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Movie not found");

        return problem;  // Spring will render this as a 404 response with JSON body
    }
}
