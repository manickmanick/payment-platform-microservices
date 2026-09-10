package com.order.order_service.controller;


import com.order.order_service.dto.CreateOrderRequest;
import com.order.order_service.dto.OrderResponse;
import com.order.order_service.entity.Order;
import com.order.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse > createOrder(@Valid @RequestBody CreateOrderRequest request){
        OrderResponse  order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(
            @PathVariable Long orderId) {

        OrderResponse  order = orderService.confirmOrder(orderId);

        return ResponseEntity.ok(order);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse > cancelOrder(
            @PathVariable Long orderId) {

        OrderResponse  order = orderService.cancelOrder(orderId);

        return ResponseEntity.ok(order);
    }

}
