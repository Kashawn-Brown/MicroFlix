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

    // Return 404 with a ProblemDetail body when a rating is not found.
    @ExceptionHandler(RatingNotFoundException.class)
    public ProblemDetail handleRatingNotFound(RatingNotFoundException ex) {
        log.info("Rating not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problem.setTitle("Rating not found");
        return problem;
    }

    // Return 400 when input data (e.g. rating value) is invalid.
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.info("Invalid input data: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle("Invalid rating request");
        return problem;
    }

}
