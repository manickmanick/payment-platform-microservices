package com.ecommerce.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidationErrors(
            MethodArgumentNotValidException ex
    ){
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach((error)->{
            fieldErrors.putIfAbsent(error.getField(),error.getDefaultMessage());
        });

        Map<String,Object> body = new LinkedHashMap<>();
        body.put("message", "Please correct the validation errors");
        body.put("errors",fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    // 2. Business errors raised using ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> handleResponseStatus(
            ResponseStatusException ex
    ){
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getReason() != null
                ? ex.getReason()
                : "Request could not be completed");

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(body);
    }

    // 3. Unexpected errors: do not expose internal exception details
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedError(
            Exception ex) {

        // Log the complete exception on the server for debugging.
        // Replace this with your project's logger if available.
//        System.err.println("Unexpected error: " + ex.getMessage());
//        ex.printStackTrace();

        log.error("Unexpected error while processing request", ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Something went wrong. Please try again later.");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

}
