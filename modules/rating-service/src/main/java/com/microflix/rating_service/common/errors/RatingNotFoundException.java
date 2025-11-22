package com.microflix.rating_service.common.errors;

/**
 * Thrown when a requested rating cannot be found.
 */
public class RatingNotFoundException extends RuntimeException{

    public RatingNotFoundException(String message) {
        super(message);
    }
}
