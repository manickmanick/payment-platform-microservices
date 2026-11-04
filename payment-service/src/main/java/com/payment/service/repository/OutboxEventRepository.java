package com.payment.service.repository;

import com.payment.service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
        SELECT *
        FROM outbox_events
        WHERE
        (
            (
                status = 'PENDING'
                AND (
                    next_attempt_at IS NULL
                    OR next_attempt_at <= :now
                )
            )
            OR
            (
                status = 'PROCESSING'
                AND claimed_at <= :staleBefore
            )
        )
        ORDER BY created_at ASC
        LIMIT 100
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> claimNextBatch(
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore
    );
}