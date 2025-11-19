package com.microflix.rating_service.common.errors;

public class RatingNotFoundException extends RuntimeException{

    public RatingNotFoundException(String message) {
        super(message);
    }
}
