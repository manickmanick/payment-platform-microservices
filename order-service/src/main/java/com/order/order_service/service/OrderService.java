
package com.order.order_service.service;

import com.order.order_service.dto.CreateOrderRequest;
import com.order.order_service.dto.OrderResponse;
import com.order.order_service.entity.Order;
import com.order.order_service.entity.OrderStatus;
import com.order.order_service.entity.OutboxEvent;
import com.order.order_service.event.OrderCreatedEvent;
import com.order.order_service.repository.OrderRepository;
import com.order.order_service.repository.OutboxEventRepository;
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

        // 1. Save the order.
        Order order = new Order();
        order.setUserId(request.userId());
        order.setAmount(request.amount());
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        // 2. Create the existing application-level event.
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getAmount(),
                "INR"
        );

        // 3. Serialize it to JSON for Outbox storage.
        String payload;

        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to serialize OrderCreatedEvent",
                    exception
            );
        }

        // 4. Save the Outbox event in the same database transaction.
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setEventId(event.eventId());
        outboxEvent.setAggregateType("ORDER");
        outboxEvent.setAggregateId(savedOrder.getId().toString());
        outboxEvent.setEventType("OrderCreatedEvent");
        outboxEvent.setTopic("order-created");
        outboxEvent.setMessageKey(savedOrder.getId().toString());
        outboxEvent.setPayload(payload);
        outboxEvent.setStatus("PENDING");
        outboxEvent.setCreatedAt(LocalDateTime.now());

        outboxEventRepository.save(outboxEvent);

        return savedOrder;
    }

    @Transactional
    public OrderResponse confirmOrder(Long orderId) {

        Order order = findOrder(orderId);
        order.setStatus(OrderStatus.CONFIRMED);

        Order updatedOrder = orderRepository.save(order);
        return toResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {

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
