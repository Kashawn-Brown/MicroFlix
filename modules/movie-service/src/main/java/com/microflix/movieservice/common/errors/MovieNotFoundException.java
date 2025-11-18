package com.microflix.movieservice.common.errors;

/**
 * Thrown when a movie with a given id cannot be found.
 */
public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(Long id) {
        super("No movie with the id " + id + " found");
    }
}
