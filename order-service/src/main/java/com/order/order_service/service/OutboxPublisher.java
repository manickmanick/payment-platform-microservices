package com.order.order_service.service;


import com.order.order_service.entity.OutboxEvent;
import com.order.order_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.springframework.kafka.core.KafkaTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents(){

        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop100ByStatusOrderByCreatedAtAsc("PENDING");


        for(OutboxEvent event: events){

            try{
                // This prevents the JSON from being sent as a quoted String.
                Object payload =
                        objectMapper.readTree(event.getPayload());


                // Wait until Kafka acknowledges the send.
                kafkaTemplate.send(
                        event.getTopic(),
                        event.getMessageKey(),
                        payload
                ).get();

                // Mark published only after Kafka acknowledges.
                event.setStatus("PUBLISHED");
                event.setPublishedAt(LocalDateTime.now());

                outboxEventRepository.save(event);

                log.info(
                        "Outbox event published: eventId={}, topic={}",
                        event.getEventId(),
                        event.getTopic()
                );

            }catch(InterruptedException exception){
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

}
