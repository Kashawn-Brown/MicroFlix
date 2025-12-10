package com.microflix.rating_service.common.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps rating-related exceptions to HTTP error responses.
 */
@RestControllerAdvice
public class RatingErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(RatingErrorAdvice.class);

    /**
     * 404 when a rating cannot be found.
     */
    @ExceptionHandler(RatingNotFoundException.class)
    public ProblemDetail handleRatingNotFound(RatingNotFoundException ex) {
        log.info("Rating not found: {}", ex.getMessage());

        var pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        pd.setTitle("Rating not found");
        return pd;
    }


    /**
     * 400 when input data (e.g. rating value) is invalid.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.info("Invalid rating request: {}", ex.getMessage());

        var pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        pd.setTitle("Invalid rating request");
        return pd;
    }

    /**
     * 500 fallback for any unexpected errors in rating-service.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail fallback(Exception ex) {
        log.error("Unhandled rating-service error", ex);

        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Error");
        pd.setDetail("Something went wrong in rating-service.");
        return pd;
    }

}
