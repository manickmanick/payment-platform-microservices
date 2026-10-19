package com.ecommerce.user_service.controller;
import jakarta.persistence.EntityNotFoundException;import java.util.Map;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler{@ExceptionHandler(EntityNotFoundException.class)ResponseEntity<Map<String,String>> missing(EntityNotFoundException e){return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message",e.getMessage()));}}
