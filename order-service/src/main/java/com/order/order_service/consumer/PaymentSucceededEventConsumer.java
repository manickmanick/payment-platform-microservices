package com.order.order_service.consumer;

import com.order.order_service.service.OrderService;
import com.payment.service.avro.PaymentSucceededEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import org.slf4j.MDC;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSucceededEventConsumer {

    private static final String CORRELATION_ID_HEADER =
            "X-Correlation-ID";

    private static final String CORRELATION_ID_MDC =
            "correlationId";

    private final OrderService orderService;

    @KafkaListener(
            topics = "payment-succeeded",
            groupId = "order-service-payment-group"
    )
    public void consume(ConsumerRecord<String, PaymentSucceededEvent> record) {

        try {
            // 1. Extract Avro event from Kafka record
            PaymentSucceededEvent event = record.value();

            // 2. Read Correlation ID from Kafka header
            Header correlationHeader = record.headers()
                    .lastHeader(CORRELATION_ID_HEADER);

            if (correlationHeader != null) {

                String correlationId = new String(
                        correlationHeader.value(),
                        StandardCharsets.UTF_8
                );

                if (!correlationId.isBlank()) {
                    MDC.put(CORRELATION_ID_MDC, correlationId);
                }
            }

            // 3. Log received event
            log.info(
                    "Payment succeeded event received. eventId={}, orderId={}",
                    event.getEventId(),
                    event.getOrderId()
            );

            // 4. Confirm order
            orderService.confirmOrder(event.getOrderId());

            // 5. Log successful processing
            log.info(
                    "Order confirmation processed. orderId={}",
                    event.getOrderId()
            );

        } finally {

            // 6. Always clean up MDC
            MDC.remove(CORRELATION_ID_MDC);
        }
    }
}