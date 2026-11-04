
package com.order.order_service.service;

import com.order.order_service.entity.OutboxEvent;
import com.order.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxClaimService {

    private static final int STALE_MINUTES = 5;

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public List<OutboxEvent> claimBatch() {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime staleBefore =
                now.minusMinutes(STALE_MINUTES);

        List<OutboxEvent> events =
                outboxEventRepository.claimNextBatch(
                        now,
                        staleBefore
                );

        for (OutboxEvent event : events) {

            event.setStatus("PROCESSING");

            event.setClaimedAt(now);

            event.setAttempts(
                    event.getAttempts() + 1
            );
        }

        return events;
    }
}
