package com.microflix.userservice.common.errors;


import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global error -> turn thrown exceptions into small, clean JSON.
 * - 400 for bad input (our IllegalArgumentException + @Valid errors)
 * - 500 for everything else (hide internals from clients)
 */
@RestControllerAdvice
public class ErrorAdvice {

    // Bad input from user (If any controller (or service called by it) throws IllegalArgumentException, this method is used)
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException ex) {

        // ProblemDetail is a Spring class that implements the RFC 7807 “problem+json” error format.
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());

        // set a title
        pd.setTitle("Bad Request");
        return pd;
    }

    // Validation failures (@Valid on DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) {

        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Failed");

        // Extracts all the validation errors made (collects them into a Map)
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (a,b) -> a)); // keep first

        // Adds map of all validation errors to custom property
        pd.setProperty("errors", errors);
        return pd;
    }

    // Safety net: catches any exception that wasn’t caught by more specific handlers above
    @ExceptionHandler(Exception.class)
    ProblemDetail fallback(Exception ex) {

        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Error");
        pd.setDetail("Something went wrong");
        return pd;
    }
}
