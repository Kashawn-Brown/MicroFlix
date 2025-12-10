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

    /**
     * 404 when a movie cannot be found.
     */
    @ExceptionHandler(MovieNotFoundException.class)
    ProblemDetail handleMovieNotFound(MovieNotFoundException ex) {

        log.info("Movie not found: {}", ex.getMessage());

        var pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        pd.setTitle("Movie not found");

        return pd;
    }

    /**
     * 400 for bad arguments (e.g. invalid query params).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.info("Invalid movie request: {}", ex.getMessage());

        var pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        pd.setTitle("Invalid movie request");
        return pd;
    }

    /**
     * 500 safety net for any unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail fallback(Exception ex) {
        log.error("Unhandled movie-service error", ex);

        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Error");
        pd.setDetail("Something went wrong in movie-service.");
        return pd;
    }
}
