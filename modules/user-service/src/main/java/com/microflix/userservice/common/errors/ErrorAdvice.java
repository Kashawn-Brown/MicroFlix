package com.microflix.userservice.common.errors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global error handler converting exceptions to ProblemDetail JSON.
 * - 400 for bad input and validation errors
 * - 500 for uncaught errors
 */
@RestControllerAdvice
public class ErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(ErrorAdvice.class);

    // 400 for IllegalArgumentException (bad input).
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException ex) {

        // ProblemDetail is a Spring class that implements the RFC 7807 “problem+json” error format.
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());

        // set a title
        pd.setTitle("Bad Request");
        return pd;
    }

    // 400 for Validation failures (@Valid on DTOs)
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
    // 500 for any other unhandled exception.
    @ExceptionHandler(Exception.class)
    ProblemDetail fallback(Exception ex) {

        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Error");
        pd.setDetail("Something went wrong");
        return pd;
    }
}
