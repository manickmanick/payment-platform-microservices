package com.payment.platform.saga_orchestrator.client;

import com.payment.platform.saga_orchestrator.dto.CreateOrderRequest;
import com.payment.platform.saga_orchestrator.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name="order-service",
        url="${order-service.url}"
)
public interface OrderClient {

    @PostMapping("/orders")
    OrderResponse createOrder(
            @RequestBody CreateOrderRequest request
    );


    @PutMapping("/orders/{orderId}/confirm")
    OrderResponse confirmOrder(
            @PathVariable Long orderId
    );

    @PutMapping("/orders/{orderId}/cancel")
    OrderResponse cancelOrder(
            @PathVariable Long orderId
    );

}
