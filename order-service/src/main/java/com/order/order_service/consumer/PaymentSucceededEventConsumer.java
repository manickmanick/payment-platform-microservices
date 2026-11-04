package com.order.order_service.consumer;

import com.order.order_service.service.OrderService;
import com.payment.service.avro.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSucceededEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "payment-succeeded",
            groupId = "order-service-payment-group"
    )
    public void consume(PaymentSucceededEvent event) {

        log.info(
                "Payment succeeded event received. eventId={}, orderId={}",
                event.getEventId(),
                event.getOrderId()
        );

        orderService.confirmOrder(event.getOrderId());

        log.info(
                "Order confirmed successfully. orderId={}",
                event.getOrderId()
        );
    }
}