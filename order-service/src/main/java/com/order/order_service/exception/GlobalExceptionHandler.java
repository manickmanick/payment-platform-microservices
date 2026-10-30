package com.order.order_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

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



        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Something went wrong. Please try again later.");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

}
