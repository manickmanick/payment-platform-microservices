package com.order.order_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;


public record Checkout(
        @NotBlank
        @Size(max = 500)
        String deliveryAddress,

        @Size(max = 40)
        String couponCode,

        @jakarta.validation.constraints.DecimalMin("-90")
        @jakarta.validation.constraints.DecimalMax("90")
        BigDecimal latitude,

        @jakarta.validation.constraints.DecimalMin("-180")
        @jakarta.validation.constraints.DecimalMax("180")
        BigDecimal longitude,

        @Size(max = 30)
        String customerPhone,

        boolean smsOptIn) {
}