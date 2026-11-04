
package com.order.order_service.service;

import com.order.order_service.avro.OrderCreatedEvent;
import com.order.order_service.entity.OutboxEvent;
import com.order.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final int MAX_ATTEMPTS = 10;

    private final OutboxClaimService claimService;
    private final OutboxEventStateService stateService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {

        List<OutboxEvent> events = claimService.claimBatch();

        if (events.isEmpty()) {
            return;
        }

        log.info("Claimed {} outbox events", events.size());

        for (OutboxEvent event : events) {

            try {
                JsonNode payload =
                        objectMapper.readTree(event.getPayload());

                OrderCreatedEvent avroEvent = toAvroEvent(payload);

                // Wait for Kafka acknowledgement.
                kafkaTemplate.send(
                        event.getTopic(),
                        event.getMessageKey(),
                        avroEvent
                ).get();

                // Update MySQL only after Kafka acknowledges.
                stateService.markPublished(event.getId());

                log.info(
                        "Outbox event published: eventId={}, topic={}, attempts={}",
                        event.getEventId(),
                        event.getTopic(),
                        event.getAttempts()
                );

            } catch (InterruptedException exception) {

                handleFailure(event, exception);

                Thread.currentThread().interrupt();

                log.warn(
                        "Outbox publishing interrupted. Stopping current batch."
                );

                return;

            } catch (ExecutionException | RuntimeException exception) {

                handleFailure(event, exception);

            } catch (Exception exception) {

                // Invalid JSON or event conversion errors also need
                // to be visible and eventually moved to FAILED.
                handleFailure(event, exception);
            }
        }
    }

    private void handleFailure(
            OutboxEvent event,
            Exception exception
    ) {

        String error = exception.getMessage();

        log.error(
                "Outbox publishing failed: eventId={}, attempt={}",
                event.getEventId(),
                event.getAttempts(),
                exception
        );

        try {

            if (event.getAttempts() >= MAX_ATTEMPTS) {

                stateService.markFailed(
                        event.getId(),
                        error
                );

                log.error(
                        "Outbox event moved to FAILED: eventId={}",
                        event.getEventId()
                );

            } else {

                stateService.scheduleRetry(
                        event.getId(),
                        error
                );

                log.info(
                        "Outbox retry scheduled: eventId={}, attempt={}",
                        event.getEventId(),
                        event.getAttempts()
                );
            }

        } catch (Exception stateException) {

            // Leave it PROCESSING. The stale-claim mechanism
            // will make it eligible again after the lease expires.
            log.error(
                    "Could not update outbox state: eventId={}",
                    event.getEventId(),
                    stateException
            );
        }
    }

    private OrderCreatedEvent toAvroEvent(JsonNode payload) {

        if (payload == null
                || payload.get("eventId") == null
                || payload.get("orderId") == null
                || payload.get("userId") == null
                || payload.get("amount") == null
                || payload.get("currency") == null) {

            throw new IllegalArgumentException(
                    "Invalid OrderCreatedEvent payload"
            );
        }

        BigDecimal amount = payload.get("amount")
                .decimalValue()
                .setScale(2, RoundingMode.UNNECESSARY);

        ByteBuffer amountBytes = ByteBuffer.wrap(
                amount.unscaledValue().toByteArray()
        );

        return OrderCreatedEvent.newBuilder()
                .setEventId(payload.get("eventId").asText())
                .setOrderId(payload.get("orderId").longValue())
                .setUserId(payload.get("userId").longValue())
                .setAmount(amountBytes)
                .setCurrency(payload.get("currency").asText())
                .build();
    }
}
