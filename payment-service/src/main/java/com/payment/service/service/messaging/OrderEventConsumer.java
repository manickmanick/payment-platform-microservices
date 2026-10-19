
package com.payment.service.service.messaging;

import com.order.order_service.avro.OrderCreatedEvent;
import com.payment.service.service.OrderEventAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import org.apache.avro.Conversions;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderEventAuditService orderEventAuditService;

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
                "INR"
        );

        log.info(
                "Order event audit persisted for orderId={}",
                event.getOrderId()
        );
    }

    private BigDecimal convertAmount(ByteBuffer bytes) {

        LogicalTypes.Decimal decimalType =
                LogicalTypes.decimal(12, 2);

        Schema decimalSchema = decimalType.addToSchema(
                Schema.create(Schema.Type.BYTES)
        );

        return new Conversions.DecimalConversion().fromBytes(
                bytes.duplicate(),
                decimalSchema,
                decimalType
        );
    }
}
