package com.payment.service.event;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        Long userId,
        BigDecimal amount
) {
}
