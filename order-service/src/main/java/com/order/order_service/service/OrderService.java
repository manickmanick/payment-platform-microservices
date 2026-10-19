
package com.order.order_service.service;

import com.order.order_service.dto.CreateOrderRequest;
import com.order.order_service.dto.OrderResponse;
import com.order.order_service.entity.Order;
import com.order.order_service.entity.OrderStatus;
import com.order.order_service.entity.OutboxEvent;
import com.order.order_service.event.OrderCreatedEvent;
import com.order.order_service.repository.OrderRepository;
import com.order.order_service.repository.OutboxEventRepository;
import com.order.order_service.repository.OrderItemRepository;
import com.order.order_service.client.DeliveryClient;
import com.order.order_service.dto.DeliveryRequest;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final OrderItemRepository orderItems;
    private final DeliveryClient deliveryClient;
    private final String deliveryInternalKey;

    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper, OrderItemRepository orderItems, DeliveryClient deliveryClient, @Value("${delivery-service.internal-key:local-internal-key-change-me}") String deliveryInternalKey) {
        this.orderRepository=orderRepository; this.outboxEventRepository=outboxEventRepository; this.objectMapper=objectMapper; this.orderItems=orderItems; this.deliveryClient=deliveryClient; this.deliveryInternalKey=deliveryInternalKey;
    }

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
        boolean alreadyConfirmed = order.getStatus() == OrderStatus.CONFIRMED;
        if(!alreadyConfirmed&&order.getStatus()!=OrderStatus.PENDING)throw new IllegalStateException("Only pending orders can be confirmed");
        order.setStatus(OrderStatus.CONFIRMED);

        Order updatedOrder = orderRepository.save(order);
        var lines=orderItems.findByOrderIdOrderById(orderId).stream().map(i->new DeliveryRequest.Item(i.getItemName(),i.getQuantity())).toList();
        if(!alreadyConfirmed&&order.getDeliveryAddress()!=null&&!order.getDeliveryAddress().isBlank()) deliveryClient.create(deliveryInternalKey,new DeliveryRequest(order.getId(),order.getUserId(),order.getCustomerEmail(),order.getCustomerPhone(),order.isSmsOptIn(),order.getDeliveryAddress(),order.getAmount(),order.getDeliveryLatitude(),order.getDeliveryLongitude(),lines));
        return toResponse(updatedOrder);
    }

    @Transactional public void applyDeliveryStatus(Long orderId,OrderStatus status){Order order=findOrder(orderId);order.setStatus(status);orderRepository.save(order);}

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
    @org.springframework.transaction.annotation.Transactional(readOnly=true) public Order getOrder(Long orderId){return findOrder(orderId);}
    @org.springframework.transaction.annotation.Transactional(readOnly=true) public boolean hasDeliveredOrder(Long userId,Long restaurantId){return orderItems.hasDeliveredOrder(userId,restaurantId);}

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus().name()
        );
    }
}
