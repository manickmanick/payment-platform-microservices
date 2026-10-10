package com.payment.service.service.messaging;


import com.payment.service.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventConsumer {

    @KafkaListener(topics = "order-created")
    public void orderEventConsumer(OrderCreatedEvent event){
        log.info(
                "Received OrderCreated event. orderId={}, userId={}, amount={}",
                event.orderId(),
                event.userId(),
                event.amount()
        );
    }
}
