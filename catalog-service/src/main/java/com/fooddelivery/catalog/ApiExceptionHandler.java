package com.fooddelivery.catalog;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    org.springframework.http.ResponseEntity<Map<String,String>> notFound(jakarta.persistence.EntityNotFoundException ex) {
        return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    Map<String,String> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField()+": "+e.getDefaultMessage()).collect(Collectors.joining("; "));
        return Map.of("message", message.isBlank() ? "Request validation failed" : message);
    }
    @ExceptionHandler(ResponseStatusException.class)
    org.springframework.http.ResponseEntity<Map<String,String>> status(ResponseStatusException ex) {
        return org.springframework.http.ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", ex.getReason() == null ? "Request failed" : ex.getReason()));
    }
}
