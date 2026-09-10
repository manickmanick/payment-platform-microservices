package com.order.order_service.service;

import com.order.order_service.client.PaymentClient;
import com.order.order_service.dto.CreateOrderRequest;
import com.order.order_service.dto.OrderResponse;
import com.order.order_service.dto.PaymentRequest;
import com.order.order_service.dto.PaymentResponse;
import com.order.order_service.entity.Order;
import com.order.order_service.entity.OrderStatus;
import com.order.order_service.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        Order order = Order.builder()
                .userId(request.userId())
                .amount(request.amount())
                .status(OrderStatus.PENDING)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
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
