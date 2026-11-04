package com.payment.service.service;

import com.payment.service.entity.OutboxEvent;
import com.payment.service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OutboxEventStateService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void markPublished(Long eventId) {

        OutboxEvent event = getEvent(eventId);

        event.setStatus("PUBLISHED");

        event.setPublishedAt(LocalDateTime.now());

        event.setClaimedAt(null);

        event.setLastError(null);

        event.setNextAttemptAt(null);

        outboxEventRepository.save(event);
    }

    @Transactional
    public void scheduleRetry(Long eventId, String errorMessage) {

        OutboxEvent event = getEvent(eventId);

        int attempts = event.getAttempts();

        long delaySeconds = Math.min(
                5L * (1L << Math.min(attempts - 1, 6)),
                300L
        );

        event.setStatus("PENDING");

        event.setNextAttemptAt(
                LocalDateTime.now().plusSeconds(delaySeconds)
        );

        event.setClaimedAt(null);

        event.setLastError(errorMessage);

        outboxEventRepository.save(event);
    }

    @Transactional
    public void markFailed(Long eventId, String errorMessage) {

        OutboxEvent event = getEvent(eventId);

        event.setStatus("FAILED");

        event.setClaimedAt(null);

        event.setLastError(errorMessage);

        event.setNextAttemptAt(null);

        outboxEventRepository.save(event);
    }

    private OutboxEvent getEvent(Long eventId) {

        return outboxEventRepository.findById(eventId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Outbox event not found: " + eventId
                        )
                );
    }
}