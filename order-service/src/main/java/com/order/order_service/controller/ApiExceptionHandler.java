package com.order.order_service.controller;
import com.order.order_service.entity.Order;import feign.FeignException;import jakarta.persistence.EntityNotFoundException;import java.util.Map;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler{
 @ExceptionHandler(EntityNotFoundException.class)ResponseEntity<Map<String,String>> missing(EntityNotFoundException e){return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(IllegalStateException.class)ResponseEntity<Map<String,String>> conflict(IllegalStateException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message",e.getMessage()));}
 @ExceptionHandler(FeignException.class)ResponseEntity<Map<String,String>> downstream(FeignException e){return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message","A downstream service is temporarily unavailable"));}
}
