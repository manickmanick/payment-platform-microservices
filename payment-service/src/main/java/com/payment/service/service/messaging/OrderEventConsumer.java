package com.payment.service.service.messaging;


import com.payment.service.dto.PaymentRequest;
import com.payment.service.event.OrderCreatedEvent;
import com.payment.service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-created")
    public void orderEventConsumer(OrderCreatedEvent event){

        log.info("Processing order event: eventId={}, orderId={}",
                event.eventId(), event.orderId());


        PaymentRequest request = new PaymentRequest(
                event.userId(),
                event.orderId(),
                event.amount(),
                "INR",
                false,
                event.eventId().toString()
        );

        paymentService.createPayment(request);

        log.info("Payment processing completed for orderId={}",event.orderId());

    }
}
