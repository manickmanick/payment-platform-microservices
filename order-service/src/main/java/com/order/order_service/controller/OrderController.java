package com.order.order_service.controller;
import com.order.order_service.dto.*;import com.order.order_service.entity.*;import com.order.order_service.service.OrderService;import jakarta.validation.Valid;import java.util.Objects;
import org.springframework.http.ResponseEntity;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/orders") public class OrderController {private final OrderService service;public OrderController(OrderService s){service=s;}
 @PostMapping public ResponseEntity<Order> create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CreateOrderRequest request){if(!request.userId().equals(uid(jwt)))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Cannot create an order for another user");return ResponseEntity.ok(service.createOrder(request));}
 @PutMapping("/{orderId}/confirm") public ResponseEntity<OrderResponse> confirm(@AuthenticationPrincipal Jwt jwt,@PathVariable Long orderId){ownerOrAdmin(jwt,orderId);return ResponseEntity.ok(service.confirmOrder(orderId));}
 private void ownerOrAdmin(Jwt jwt,Long id){Order o=service.getOrder(id);String role=jwt.getClaimAsString("role");if(!Objects.equals(o.getUserId(),uid(jwt))&&!"ROLE_ADMIN".equals(role))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Cannot access this order");}
 private long uid(Jwt jwt){return Long.parseLong(jwt.getClaim("userId").toString());}
}
