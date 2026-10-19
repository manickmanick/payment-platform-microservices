package com.fooddelivery.delivery;
import feign.FeignException;import jakarta.persistence.EntityNotFoundException;import java.util.Map;import org.springframework.http.*;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class DeliveryExceptionHandler{
 @ExceptionHandler(EntityNotFoundException.class) ResponseEntity<Map<String,String>> missing(EntityNotFoundException e){return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(IllegalStateException.class) ResponseEntity<Map<String,String>> conflict(IllegalStateException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(SecurityException.class) ResponseEntity<Map<String,String>> forbidden(SecurityException e){return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(FeignException.class) ResponseEntity<Map<String,String>> downstream(FeignException e){return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message","Order service is temporarily unavailable"));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<Map<String,String>> invalid(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Map.of("message","Request validation failed"));}
}
