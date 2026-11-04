package com.payment.service.service;

import com.payment.service.entity.OutboxEvent;
import com.payment.service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public List<OutboxEvent> claimBatch() {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime staleBefore = now.minusMinutes(2);

        List<OutboxEvent> events =
                outboxEventRepository.claimNextBatch(now, staleBefore);

        for (OutboxEvent event : events) {

            event.setStatus("PROCESSING");

            event.setClaimedAt(now);

            event.setAttempts(event.getAttempts() + 1);
        }

        outboxEventRepository.saveAll(events);

        return events;
    }
}