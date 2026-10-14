
package com.payment.service.service.messaging;

import com.order.order_service.avro.OrderCreatedEvent;
import com.payment.service.dto.PaymentRequest;
import com.payment.service.service.PaymentService;
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

    private final PaymentService paymentService;

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

        PaymentRequest request = new PaymentRequest(
                event.getUserId(),
                event.getOrderId(),
                amount,
                "INR",
                false,
                event.getEventId().toString()
        );

        paymentService.createPayment(request);

        log.info(
                "Payment processing completed for orderId={}",
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
