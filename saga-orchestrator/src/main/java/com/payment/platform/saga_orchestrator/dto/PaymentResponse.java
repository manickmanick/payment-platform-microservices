package com.payment.platform.saga_orchestrator.dto;

public record PaymentResponse(
        Long paymentId,
        String status
) {
}
