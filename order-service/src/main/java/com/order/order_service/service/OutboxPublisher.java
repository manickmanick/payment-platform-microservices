
package com.order.order_service.service;

import com.order.order_service.avro.OrderCreatedEvent;
import com.order.order_service.avro.OrderConfirmedEvent;
import com.order.order_service.avro.OrderItem;
import com.order.order_service.entity.OutboxEvent;

import lombok.extern.slf4j.Slf4j;

import org.apache.avro.Conversions;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.nio.ByteBuffer;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ExecutionException;

@Component
@Slf4j
public class OutboxPublisher {

    private static final int MAX_ATTEMPTS = 10;

    private final OutboxClaimService claimService;
    private final OutboxEventStateService stateService;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTemplate<String, String> dltKafkaTemplate;

    private final ObjectMapper objectMapper;

    private static final Conversions.DecimalConversion DECIMAL_CONVERSION =
            new Conversions.DecimalConversion();

    private static final Schema CONFIRMED_EVENT_SCHEMA =
            OrderConfirmedEvent.getClassSchema();

    private static final Schema AMOUNT_SCHEMA =
            findDecimalSchema("amount");

    private static final Schema LATITUDE_SCHEMA =
            findDecimalSchema("deliveryLatitude");

    private static final Schema LONGITUDE_SCHEMA =
            findDecimalSchema("deliveryLongitude");


    // --------------------------------------------------
    // Constructor
    // --------------------------------------------------

    public OutboxPublisher(
            OutboxClaimService claimService,
            OutboxEventStateService stateService,

            @Qualifier("avroKafkaTemplate")
            KafkaTemplate<String, Object> kafkaTemplate,

            @Qualifier("dltKafkaTemplate")
            KafkaTemplate<String, String> dltKafkaTemplate,

            ObjectMapper objectMapper
    ) {

        this.claimService = claimService;
        this.stateService = stateService;
        this.kafkaTemplate = kafkaTemplate;
        this.dltKafkaTemplate = dltKafkaTemplate;
        this.objectMapper = objectMapper;
    }


    // --------------------------------------------------
    // 1. Find Decimal schema from Avro field
    // --------------------------------------------------

    private static Schema findDecimalSchema(String fieldName) {

        Schema fieldSchema =
                CONFIRMED_EVENT_SCHEMA.getField(fieldName).schema();

        if (fieldSchema.getType() == Schema.Type.UNION) {

            return fieldSchema.getTypes()
                    .stream()
                    .filter(schema ->
                            schema.getLogicalType()
                                    instanceof LogicalTypes.Decimal)
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Decimal schema not found: " + fieldName
                            )
                    );
        }

        if (!(fieldSchema.getLogicalType()
                instanceof LogicalTypes.Decimal)) {

            throw new IllegalStateException(
                    "Field is not an Avro Decimal: " + fieldName
            );
        }

        return fieldSchema;
    }


    // --------------------------------------------------
    // 2. Publish pending Outbox events
    // --------------------------------------------------

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {

        List<OutboxEvent> events = claimService.claimBatch();

        if (events.isEmpty()) {
            return;
        }

        log.info("Claimed {} outbox events", events.size());

        for (OutboxEvent event : events) {

            /*
             * Once normal publishing attempts reach MAX_ATTEMPTS,
             * do not try the original topic again.
             *
             * Retry DLT publishing only.
             */

            if (event.getAttempts() >= MAX_ATTEMPTS) {

                String error = event.getLastError();

                if (error == null || error.isBlank()) {
                    error = "Maximum normal publishing attempts reached";
                }

                publishToDlt(event, error);

                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                continue;
            }

            try {

                publishNormalEvent(event);

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

                handleFailure(event, exception);
            }
        }
    }


    // --------------------------------------------------
    // 3. Normal Avro event publishing
    // --------------------------------------------------

    private void publishNormalEvent(OutboxEvent event)
            throws Exception {

        JsonNode payload =
                objectMapper.readTree(event.getPayload());

        Object avroEvent = switch (event.getEventType()) {

            case "OrderCreatedEvent" ->
                    toAvroOrderCreatedEvent(payload);

            case "OrderConfirmedEvent" ->
                    toAvroOrderConfirmedEvent(payload);

            default -> throw new IllegalArgumentException(
                    "Unsupported event type: " + event.getEventType()
            );
        };

        // Wait for Kafka acknowledgement
        kafkaTemplate.send(
                event.getTopic(),
                event.getMessageKey(),
                avroEvent
        ).get();

        // Update database only after Kafka acknowledgement
        stateService.markPublished(event.getId());

        log.info(
                "Outbox event published successfully: eventId={}, topic={}, attempts={}",
                event.getEventId(),
                event.getTopic(),
                event.getAttempts()
        );
    }


    // --------------------------------------------------
    // 4. Publish failed event to DLT
    // --------------------------------------------------

    private void publishToDlt(
            OutboxEvent event,
            String originalFailure
    ) {

        String dltTopic = event.getTopic() + ".DLT";

        try {

            Map<String, Object> envelope = new LinkedHashMap<>();

            envelope.put("eventId", event.getEventId());
            envelope.put("eventType", event.getEventType());

            envelope.put("aggregateType", event.getAggregateType());
            envelope.put("aggregateId", event.getAggregateId());

            envelope.put("originalTopic", event.getTopic());
            envelope.put("dltTopic", dltTopic);

            envelope.put("messageKey", event.getMessageKey());

            // Preserve original event payload
            envelope.put(
                    "payload",
                    objectMapper.readTree(event.getPayload())
            );

            envelope.put("attempts", event.getAttempts());
            envelope.put("failureReason", originalFailure);

            envelope.put(
                    "failedAt",
                    LocalDateTime.now().toString()
            );

            String dltMessage =
                    objectMapper.writeValueAsString(envelope);

            log.warn(
                    "Publishing event to DLT: eventId={}, dltTopic={}, attempts={}",
                    event.getEventId(),
                    dltTopic,
                    event.getAttempts()
            );

            // Publish JSON message and wait for Kafka acknowledgement
            dltKafkaTemplate.send(
                    dltTopic,
                    event.getMessageKey(),
                    dltMessage
            ).get();

            /*
             * DLT publish succeeded.
             * Only now mark the database event as FAILED.
             */

            stateService.markDltPublished(
                    event.getId(),
                    originalFailure
            );

            log.error(
                    "Event successfully moved to DLT: eventId={}, dltTopic={}",
                    event.getEventId(),
                    dltTopic
            );

        } catch (InterruptedException exception) {

            handleDltPublishFailure(event, originalFailure, exception);

            Thread.currentThread().interrupt();

        } catch (Exception exception) {

            handleDltPublishFailure(event, originalFailure, exception);
        }
    }


    // --------------------------------------------------
    // 5. Handle DLT publishing failure
    // --------------------------------------------------

    private void handleDltPublishFailure(
            OutboxEvent event,
            String originalFailure,
            Exception dltException
    ) {

        String dltError = dltException.getMessage();

        String combinedError =
                "Original publishing failure: " + originalFailure
                        + " | DLT publishing failure: " + dltError;

        log.error(
                "DLT publishing failed: eventId={}, dltTopic={}, attempts={}",
                event.getEventId(),
                event.getTopic() + ".DLT",
                event.getAttempts(),
                dltException
        );

        try {

            /*
             * Keep event retryable.
             *
             * On the next claim, attempts will still be >= MAX_ATTEMPTS,
             * so publishPendingEvents() will retry DLT only.
             */

            stateService.scheduleRetry(
                    event.getId(),
                    combinedError
            );

            log.warn(
                    "DLT retry scheduled: eventId={}, attempts={}",
                    event.getEventId(),
                    event.getAttempts()
            );

        } catch (Exception stateException) {

            log.error(
                    "Could not schedule DLT retry: eventId={}",
                    event.getEventId(),
                    stateException
            );
        }
    }


    // --------------------------------------------------
    // 6. Convert OrderCreatedEvent
    // --------------------------------------------------

    private OrderCreatedEvent toAvroOrderCreatedEvent(
            JsonNode payload
    ) {

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


    // --------------------------------------------------
    // 7. Convert OrderConfirmedEvent
    // --------------------------------------------------

    private OrderConfirmedEvent toAvroOrderConfirmedEvent(
            JsonNode payload
    ) {

        if (payload == null
                || payload.get("eventId") == null
                || payload.get("orderId") == null
                || payload.get("userId") == null
                || payload.get("deliveryAddress") == null
                || payload.get("amount") == null
                || payload.get("items") == null) {

            throw new IllegalArgumentException(
                    "Invalid OrderConfirmedEvent payload"
            );
        }

        // Amount: Avro schema decides precision and scale
        BigDecimal amount =
                payload.get("amount").decimalValue();

        ByteBuffer amountBytes = toAvroDecimal(
                amount,
                AMOUNT_SCHEMA
        );

        // Convert items
        List<OrderItem> items = new ArrayList<>();

        for (JsonNode itemNode : payload.get("items")) {

            if (itemNode.get("itemName") == null
                    || itemNode.get("quantity") == null) {

                throw new IllegalArgumentException(
                        "Invalid OrderItem payload"
                );
            }

            OrderItem item = OrderItem.newBuilder()
                    .setItemName(itemNode.get("itemName").asText())
                    .setQuantity(itemNode.get("quantity").intValue())
                    .build();

            items.add(item);
        }

        // Nullable GPS coordinates
        ByteBuffer latitude = null;
        ByteBuffer longitude = null;

        JsonNode latitudeNode =
                payload.get("deliveryLatitude");

        JsonNode longitudeNode =
                payload.get("deliveryLongitude");

        if (latitudeNode != null && !latitudeNode.isNull()) {

            latitude = toAvroDecimal(
                    latitudeNode.decimalValue(),
                    LATITUDE_SCHEMA
            );
        }

        if (longitudeNode != null && !longitudeNode.isNull()) {

            longitude = toAvroDecimal(
                    longitudeNode.decimalValue(),
                    LONGITUDE_SCHEMA
            );
        }

        // Nullable email and phone
        JsonNode emailNode =
                payload.get("customerEmail");

        JsonNode phoneNode =
                payload.get("customerPhone");

        JsonNode smsOptInNode =
                payload.get("smsOptIn");

        if (smsOptInNode == null || smsOptInNode.isNull()) {

            throw new IllegalArgumentException(
                    "Missing smsOptIn in OrderConfirmedEvent payload"
            );
        }

        return OrderConfirmedEvent.newBuilder()

                .setEventId(
                        payload.get("eventId").asText()
                )

                .setOrderId(
                        payload.get("orderId").longValue()
                )

                .setUserId(
                        payload.get("userId").longValue()
                )

                .setCustomerEmail(
                        emailNode == null || emailNode.isNull()
                                ? null
                                : emailNode.asText()
                )

                .setCustomerPhone(
                        phoneNode == null || phoneNode.isNull()
                                ? null
                                : phoneNode.asText()
                )

                .setSmsOptIn(
                        smsOptInNode.asBoolean()
                )

                .setDeliveryAddress(
                        payload.get("deliveryAddress").asText()
                )

                .setAmount(amountBytes)

                .setDeliveryLatitude(latitude)

                .setDeliveryLongitude(longitude)

                .setItems(items)

                .build();
    }


    // --------------------------------------------------
    // 8. BigDecimal to Avro Decimal conversion
    // --------------------------------------------------

    private ByteBuffer toAvroDecimal(
            BigDecimal value,
            Schema decimalSchema
    ) {

        LogicalTypes.Decimal decimalType =
                (LogicalTypes.Decimal) decimalSchema.getLogicalType();

        BigDecimal scaledValue = value.setScale(
                decimalType.getScale(),
                RoundingMode.UNNECESSARY
        );

        return DECIMAL_CONVERSION.toBytes(
                scaledValue,
                decimalSchema,
                decimalType
        );
    }


    // --------------------------------------------------
    // 9. Normal publishing retry handling
    // --------------------------------------------------

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

                // All normal attempts exhausted.
                // Start DLT publishing.
                publishToDlt(event, error);

            } else {

                // Normal retry with exponential backoff.
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

            log.error(
                    "Could not update outbox state: eventId={}",
                    event.getEventId(),
                    stateException
            );
        }
    }
}
