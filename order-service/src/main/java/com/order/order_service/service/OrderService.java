package com.order.order_service.service;

import com.order.order_service.client.PaymentClient;
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
    public Order createOrder(Long userId,
                             java.math.BigDecimal amount) {

        Order order = Order.builder()
                .userId(userId)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        PaymentRequest paymentRequest = new PaymentRequest(
                userId,
                savedOrder.getId(),
                amount,
                "INR"
        );

        PaymentResponse paymentResponse =
                paymentClient.createPayment(paymentRequest);

        if ("SUCCESS".equals(paymentResponse.status())) {
            savedOrder.setStatus(OrderStatus.CONFIRMED);
        } else {
            savedOrder.setStatus(OrderStatus.CANCELLED);
        }

        return orderRepository.save(savedOrder);
    }

}
