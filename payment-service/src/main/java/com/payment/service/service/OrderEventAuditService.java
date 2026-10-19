package com.payment.service.service;

import com.payment.service.entity.OrderEventReceipt;
import com.payment.service.repository.OrderEventReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderEventAuditService {
    private final OrderEventReceiptRepository repository;

    @Transactional
    public void record(String eventId, Long orderId, Long userId, BigDecimal amount, String currency) {
        if (repository.existsById(eventId)) return;
        try {
            repository.saveAndFlush(OrderEventReceipt.builder()
                    .eventId(eventId)
                    .orderId(orderId)
                    .userId(userId)
                    .amount(amount)
                    .currency(currency)
                    .receivedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException duplicateDelivery) {
            // Kafka can redeliver an event concurrently; the event ID is the durable idempotency key.
        }
    }
}
