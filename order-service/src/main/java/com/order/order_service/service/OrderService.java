package com.order.order_service.service;

import com.order.order_service.client.PaymentClient;
import com.order.order_service.dto.CreateOrderRequest;
import com.order.order_service.dto.OrderResponse;
import com.order.order_service.dto.PaymentRequest;
import com.order.order_service.dto.PaymentResponse;
import com.order.order_service.entity.Order;
import com.order.order_service.entity.OrderStatus;
import com.order.order_service.entity.OutboxEvent;
import com.order.order_service.event.OrderCreatedEvent;
import com.order.order_service.repository.OrderRepository;
import com.order.order_service.repository.OutboxEventRepository;
import com.order.order_service.service.messaging.OrderEventProducer;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {


        // 1. Save the order in MySQL.
        Order order = new Order();

        order.setUserId(request.userId());
        order.setAmount(request.amount());
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);


        // 2. Create the event with a unique event ID.

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getAmount()
        );

        // 3. Serialize the event into JSON.
        String payload;

        try {

            payload = objectMapper.writeValueAsString(event);


        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to serialize OrderCreatedEvent", exception);
        }

        // 4. Persist the event in the outbox table.
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventId(event.eventId().toString());
        outboxEvent.setAggregateType("ORDER");
        outboxEvent.setAggregateId(savedOrder.getId().toString());
        outboxEvent.setEventType("OrderCreatedEvent");
        outboxEvent.setTopic("order-created");
        outboxEvent.setMessageKey(savedOrder.getId().toString());
        outboxEvent.setPayload(payload);
        outboxEvent.setStatus("PENDING");

        outboxEventRepository.save(outboxEvent);

        // 5. Both database changes commit together.
        // Kafka publishing will be handled by a separate publisher.
        return savedOrder;


    }


    @Transactional
    public OrderResponse  confirmOrder(Long orderId) {

        Order order = findOrder(orderId);

        order.setStatus(OrderStatus.CONFIRMED);

        Order updatedOrder = orderRepository.save(order);

        return toResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse  cancelOrder(Long orderId) {

        Order order = findOrder(orderId);

        order.setStatus(OrderStatus.CANCELLED);

        Order updatedOrder = orderRepository.save(order);

        return toResponse(updatedOrder);
    }

    private Order findOrder(Long orderId) {

        return orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Order not found: " + orderId
                        )
                );
    }

    private OrderResponse toResponse(Order order) {

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus().name()
        );
    }


}
