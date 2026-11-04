package com.payment.service.service.messaging;

import com.order.order_service.avro.OrderCreatedEvent;
import com.payment.service.service.OrderEventAuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.avro.Conversions;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderEventAuditService orderEventAuditService;

    private static final LogicalTypes.Decimal DECIMAL_TYPE =
            LogicalTypes.decimal(12, 2);

    private static final Schema DECIMAL_SCHEMA =
            DECIMAL_TYPE.addToSchema(
                    Schema.create(Schema.Type.BYTES)
            );

    private static final Conversions.DecimalConversion DECIMAL_CONVERSION =
            new Conversions.DecimalConversion();

    @KafkaListener(
            topics = "order-created",
            groupId = "payment-service-group"
    )
    public void orderEventConsumer(OrderCreatedEvent event) {

        log.info(
                "Processing Avro order event: eventId={}, orderId={}",
                event.getEventId(),
                event.getOrderId()
        );

        BigDecimal amount = convertAmount(event.getAmount());

        orderEventAuditService.record(
                event.getEventId().toString(),
                event.getOrderId(),
                event.getUserId(),
                amount,
                event.getCurrency().toString()
        );

        log.info(
                "Order event audit persisted for orderId={}",
                event.getOrderId()
        );
    }

    private BigDecimal convertAmount(ByteBuffer bytes) {

        return DECIMAL_CONVERSION.fromBytes(
                bytes.duplicate(),
                DECIMAL_SCHEMA,
                DECIMAL_TYPE
        );
    }
}