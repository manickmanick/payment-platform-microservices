package com.payment.service.dto;

public record PaymentResponse(
        Long paymentId,
        String status
) {
}
