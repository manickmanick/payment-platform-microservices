package com.payment.service.service;

import com.payment.service.avro.PaymentSucceededEvent;
import com.payment.service.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class OutboxPublisher {

    private static final int MAX_ATTEMPTS = 10;
    private final OutboxClaimService outboxClaimService;
    private final OutboxEventStateService outboxEventStateService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OutboxPublisher(
            OutboxClaimService outboxClaimService,
            OutboxEventStateService outboxEventStateService,
            ObjectMapper objectMapper,
            @Qualifier("paymentAvroKafkaTemplate")
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.outboxClaimService = outboxClaimService;
        this.outboxEventStateService = outboxEventStateService;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }


    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {

        List<OutboxEvent> events = outboxClaimService.claimBatch();

        for (OutboxEvent event : events) {

            try {

                PaymentSucceededEvent avroEvent =
                        toAvroEvent(event.getPayload());

                ProducerRecord<String, Object> producerRecord =
                        new ProducerRecord<>(
                                event.getTopic(),
                                event.getMessageKey(),
                                avroEvent
                        );

                String correlationId = event.getCorrelationId();

                if (correlationId != null && !correlationId.isBlank()) {

                    producerRecord.headers().add(
                            new RecordHeader(
                                    "X-Correlation-ID",
                                    correlationId.getBytes(StandardCharsets.UTF_8)
                            )
                    );
                }

                kafkaTemplate.send(producerRecord).get();

//                kafkaTemplate.send(
//                        event.getTopic(),
//                        event.getMessageKey(),
//                        avroEvent
//                ).get();

                outboxEventStateService.markPublished(event.getId());

                log.info(
                        "Outbox event published. eventId={}, topic={}",
                        event.getEventId(),
                        event.getTopic()
                );

            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();

                handleFailure(event, exception);

                return;

            } catch (ExecutionException | RuntimeException exception) {

                handleFailure(event, exception);

            } catch (Exception exception) {

                handleFailure(event, exception);
            }
        }
    }

    private PaymentSucceededEvent toAvroEvent(String payload)
            throws Exception {

        JsonNode json = objectMapper.readTree(payload);

        BigDecimal amount = json.get("amount")
                .decimalValue()
                .setScale(2, RoundingMode.UNNECESSARY);

        ByteBuffer amountBytes = ByteBuffer.wrap(
                amount.unscaledValue().toByteArray()
        );

        return PaymentSucceededEvent.newBuilder()
                .setEventId(json.get("eventId").asText())
                .setOrderId(json.get("orderId").asLong())
                .setPaymentId(json.get("paymentId").asLong())
                .setUserId(json.get("userId").asLong())
                .setAmount(amountBytes)
                .setCurrency(json.get("currency").asText())
                .build();
    }

    private void handleFailure(
            OutboxEvent event,
            Exception exception
    ) {

        String errorMessage = exception.getMessage() != null
                ? exception.getMessage()
                : exception.getClass().getSimpleName();

        log.error(
                "Failed to publish outbox event. eventId={}, attempt={}",
                event.getEventId(),
                event.getAttempts(),
                exception
        );

        if (event.getAttempts() >= MAX_ATTEMPTS) {

            outboxEventStateService.markFailed(
                    event.getId(),
                    errorMessage
            );

        } else {

            outboxEventStateService.scheduleRetry(
                    event.getId(),
                    errorMessage
            );
        }
    }
}