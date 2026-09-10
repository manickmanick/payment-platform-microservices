package com.order.order_service.dto;

public record PaymentResponse(
        Long paymentId,
        String status
) {
}
