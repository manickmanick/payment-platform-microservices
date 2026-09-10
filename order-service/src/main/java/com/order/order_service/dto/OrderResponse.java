package com.order.order_service.dto;

public record OrderResponse(
        Long orderId,
        Long userId,
        String status
) {
}
