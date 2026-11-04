
package com.order.order_service.service;

import com.order.order_service.entity.OutboxEvent;
import com.order.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxEventStateService {

    private final OutboxEventRepository repository;

    @Transactional
    public void markPublished(Long eventId) {

        OutboxEvent event = getEvent(eventId);

        event.setStatus("PUBLISHED");
        event.setPublishedAt(LocalDateTime.now());
        event.setClaimedAt(null);
        event.setNextAttemptAt(null);
        event.setLastError(null);
    }

    @Transactional
    public void scheduleRetry(Long eventId, String error) {

        OutboxEvent event = getEvent(eventId);

        event.setStatus("PENDING");
        event.setClaimedAt(null);
        event.setNextAttemptAt(LocalDateTime.now().plusSeconds(
                calculateBackoffSeconds(event.getAttempts())
        ));
        event.setLastError(truncate(error));
    }

    @Transactional
    public void markFailed(Long eventId, String error) {

        OutboxEvent event = getEvent(eventId);

        event.setStatus("FAILED");
        event.setClaimedAt(null);
        event.setNextAttemptAt(null);
        event.setLastError(truncate(error));
    }

    private OutboxEvent getEvent(Long eventId) {
        return repository.findById(eventId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Outbox event not found: " + eventId
                        )
                );
    }

    private long calculateBackoffSeconds(int attempts) {

        // 2, 4, 8, 16, 32 ... maximum 300 seconds.
        int exponent = Math.min(Math.max(attempts, 1), 8);

        return Math.min(2L * (1L << (exponent - 1)), 300L);
    }

    private String truncate(String error) {

        if (error == null) {
            return "Unknown publishing error";
        }

        return error.length() > 5000
                ? error.substring(0, 5000)
                : error;
    }
}
