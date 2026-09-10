package com.payment.platform.saga_orchestrator.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SagaOrderRequest(
        @NotNull
        Long userId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        boolean simulatePaymentFailure
) {
}
