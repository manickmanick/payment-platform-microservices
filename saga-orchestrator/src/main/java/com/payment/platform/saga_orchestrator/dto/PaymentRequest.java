package com.payment.platform.saga_orchestrator.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull
        Long userId,

        @NotNull
        Long orderId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        @NotBlank
        String currency,

        boolean simulateFailure,

        String idempotencyKey

) {
}
