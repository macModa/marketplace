package com.marketplace.delivery.api;

import com.marketplace.delivery.exception.DeliveryNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for delivery endpoints.
 * Returns RFC 7807 Problem Detail responses with HTTP 4xx codes.
 */
@RestControllerAdvice(assignableTypes = DeliveryController.class)
public class DeliveryExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DeliveryExceptionHandler.class);

    @ExceptionHandler(DeliveryNotFoundException.class)
    public ProblemDetail handleNotFound(DeliveryNotFoundException ex) {
        log.warn("Delivery resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Delivery Resource Not Found");
        problem.setType(URI.create("/errors/delivery/not-found"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        log.warn("Bad delivery request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Delivery Request");
        problem.setType(URI.create("/errors/delivery/bad-request"));
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflict(IllegalStateException ex) {
        log.warn("Delivery state conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Delivery State Conflict");
        problem.setType(URI.create("/errors/delivery/conflict"));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Delivery validation failed: {}", details);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, details);
        problem.setTitle("Validation Failed");
        problem.setType(URI.create("/errors/delivery/validation"));
        return problem;
    }
}
