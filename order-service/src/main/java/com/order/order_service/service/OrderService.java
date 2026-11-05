
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
import com.order.order_service.avro.OrderConfirmedEvent;

import org.apache.avro.Conversions;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.List;
import com.order.order_service.avro.OrderConfirmedEvent;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final OrderItemRepository orderItems;

    private static final Conversions.DecimalConversion DECIMAL_CONVERSION =
            new Conversions.DecimalConversion();

    private static final Schema DECIMAL_SCHEMA =
            OrderConfirmedEvent.getClassSchema()
                    .getField("amount")
                    .schema();

    private static final LogicalTypes.Decimal DECIMAL_TYPE =
            (LogicalTypes.Decimal) DECIMAL_SCHEMA.getLogicalType();

    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper, OrderItemRepository orderItems, DeliveryClient deliveryClient, @Value("${delivery-service.internal-key:local-internal-key-change-me}") String deliveryInternalKey) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.orderItems = orderItems;
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

//    @Transactional
//    public OrderResponse confirmOrder(Long orderId) {
//
//        Order order = findOrder(orderId);
//        boolean alreadyConfirmed = order.getStatus() == OrderStatus.CONFIRMED;
//        if (!alreadyConfirmed && order.getStatus() != OrderStatus.PENDING)
//            throw new IllegalStateException("Only pending orders can be confirmed");
//        order.setStatus(OrderStatus.CONFIRMED);
//
//        Order updatedOrder = orderRepository.save(order);
//        var lines = orderItems.findByOrderIdOrderById(orderId).stream().map(i -> new DeliveryRequest.Item(i.getItemName(), i.getQuantity())).toList();
//        if (!alreadyConfirmed && order.getDeliveryAddress() != null && !order.getDeliveryAddress().isBlank())
//            deliveryClient.create(deliveryInternalKey, new DeliveryRequest(order.getId(), order.getUserId(), order.getCustomerEmail(), order.getCustomerPhone(), order.isSmsOptIn(), order.getDeliveryAddress(), order.getAmount(), order.getDeliveryLatitude(), order.getDeliveryLongitude(), lines));
//        return toResponse(updatedOrder);
//    }
@Transactional
public OrderResponse confirmOrder(Long orderId) {

    // 1. Find order
    Order order = findOrder(orderId);

    // 2. Duplicate event protection
    if (order.getStatus() == OrderStatus.CONFIRMED) {
        return toResponse(order);
    }

    if (order.getStatus() != OrderStatus.PENDING) {
        throw new IllegalStateException(
                "Only pending orders can be confirmed"
        );
    }

    // 3. Confirm order
    order.setStatus(OrderStatus.CONFIRMED);

    Order savedOrder = orderRepository.save(order);

    // 4. Check delivery address
    if (savedOrder.getDeliveryAddress() != null
            && !savedOrder.getDeliveryAddress().isBlank()) {

        // 5. Convert order items into JSON-friendly objects
        List<Map<String, Object>> items =
                orderItems.findByOrderIdOrderById(orderId)
                        .stream()
                        .map(item -> {
                            Map<String, Object> itemMap =
                                    new LinkedHashMap<>();

                            itemMap.put(
                                    "itemName",
                                    item.getItemName()
                            );

                            itemMap.put(
                                    "quantity",
                                    item.getQuantity()
                            );

                            return itemMap;
                        })
                        .toList();

        // 6. Create event payload
        Map<String, Object> payload =
                new LinkedHashMap<>();

        String eventId = UUID.randomUUID().toString();

        payload.put("eventId", eventId);
        payload.put("orderId", savedOrder.getId());
        payload.put("userId", savedOrder.getUserId());
        payload.put("customerEmail", savedOrder.getCustomerEmail());
        payload.put("customerPhone", savedOrder.getCustomerPhone());
        payload.put("smsOptIn", savedOrder.isSmsOptIn());
        payload.put(
                "deliveryAddress",
                savedOrder.getDeliveryAddress()
        );
        payload.put("amount", savedOrder.getAmount());
        payload.put(
                "deliveryLatitude",
                savedOrder.getDeliveryLatitude()
        );
        payload.put(
                "deliveryLongitude",
                savedOrder.getDeliveryLongitude()
        );
        payload.put("items", items);

        // 7. Save event in Outbox
        saveOrderConfirmedEvent(payload, eventId, orderId);
    }

    return toResponse(savedOrder);
}

    @Transactional
    public void applyDeliveryStatus(Long orderId, OrderStatus status) {
        Order order = findOrder(orderId);
        order.setStatus(status);
        orderRepository.save(order);
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

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return findOrder(orderId);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public boolean hasDeliveredOrder(Long userId, Long restaurantId) {
        return orderItems.hasDeliveredOrder(userId, restaurantId);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus().name()
        );
    }

    private void saveOrderConfirmedEvent(
            Map<String, Object> payload,
            String eventId,
            Long orderId
    ) {

        try {

            String jsonPayload =
                    objectMapper.writeValueAsString(payload);

            OutboxEvent outboxEvent = new OutboxEvent();

            outboxEvent.setEventId(eventId);
            outboxEvent.setAggregateType("ORDER");
            outboxEvent.setAggregateId(orderId.toString());
            outboxEvent.setEventType("OrderConfirmedEvent");
            outboxEvent.setTopic("order-confirmed");
            outboxEvent.setMessageKey(orderId.toString());
            outboxEvent.setPayload(jsonPayload);
            outboxEvent.setStatus("PENDING");
            outboxEvent.setCreatedAt(LocalDateTime.now());

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to save OrderConfirmedEvent into Outbox",
                    e
            );
        }
    }

    private ByteBuffer toAvroDecimal(BigDecimal value) {

        return DECIMAL_CONVERSION.toBytes(
                value,
                DECIMAL_SCHEMA,
                DECIMAL_TYPE
        );
    }
}
