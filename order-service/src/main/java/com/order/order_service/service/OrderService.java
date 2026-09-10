package com.order.order_service.service;

import com.order.order_service.client.PaymentClient;
import com.order.order_service.dto.CreateOrderRequest;
import com.order.order_service.dto.PaymentRequest;
import com.order.order_service.dto.PaymentResponse;
import com.order.order_service.entity.Order;
import com.order.order_service.entity.OrderStatus;
import com.order.order_service.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {

        // 1. Create order in PENDING state
        Order order = Order.builder()
                .userId(request.userId())
                .amount(request.amount())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 2. Prepare payment request
        PaymentRequest paymentRequest = new PaymentRequest(
                request.userId(),
                savedOrder.getId(),
                request.amount(),
                "INR"
        );

        // 3. Call Payment Service
        PaymentResponse paymentResponse =
                paymentClient.createPayment(paymentRequest);

        // 4. Update order based on payment result
        if ("SUCCESS".equals(paymentResponse.status())) {
            savedOrder.setStatus(OrderStatus.CONFIRMED);
        } else {
            savedOrder.setStatus(OrderStatus.CANCELLED);
        }

        // 5. Save updated order
        return orderRepository.save(savedOrder);
    }

}
