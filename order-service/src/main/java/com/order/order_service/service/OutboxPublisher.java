
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
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop100ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : events) {

            try {
                // 1. Read the JSON payload stored in the Outbox.
                JsonNode payload =
                        objectMapper.readTree(event.getPayload());

                // 2. Convert JSON into the generated Avro record.
                OrderCreatedEvent avroEvent = toAvroEvent(payload);

                // 3. Send the Avro record and wait for Kafka's acknowledgement.
                kafkaTemplate.send(
                        event.getTopic(),
                        event.getMessageKey(),
                        avroEvent
                ).get();

                // 4. Mark the Outbox event as published only after success.
                event.setStatus("PUBLISHED");
                event.setPublishedAt(LocalDateTime.now());

                outboxEventRepository.save(event);

                log.info(
                        "Outbox event published: eventId={}, topic={}",
                        event.getEventId(),
                        event.getTopic()
                );

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                log.error(
                        "Outbox publishing interrupted for eventId={}",
                        event.getEventId(),
                        exception
                );

                return;

            } catch (ExecutionException | RuntimeException exception) {
                log.error(
                        "Failed to publish outbox eventId={}; it remains eligible for retry",
                        event.getEventId(),
                        exception
                );
            }
        }
    }

    private OrderCreatedEvent toAvroEvent(JsonNode payload) {

        BigDecimal amount = payload.get("amount").decimalValue();

        // Avro decimal: encode the unscaled integer as a signed byte sequence.
        ByteBuffer amountBytes = ByteBuffer.wrap(
                amount.movePointRight(2)
                        .toBigIntegerExact()
                        .toByteArray()
        );

        return OrderCreatedEvent.newBuilder()
                .setEventId(payload.get("eventId").asText())
                .setOrderId(payload.get("orderId").longValue())
                .setUserId(payload.get("userId").longValue())
                .setAmount(amountBytes)
                .build();
    }
}
