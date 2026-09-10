package com.payment.platform.saga_orchestrator.dto;

public record OrderResponse(
        Long orderId,
        Long userId,
        String status
) {
}
