package com.payment.platform.saga_orchestrator.service;


import com.payment.platform.saga_orchestrator.client.OrderClient;
import com.payment.platform.saga_orchestrator.client.PaymentClient;
import com.payment.platform.saga_orchestrator.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import io.github.resilience4j.retry.annotation.Retry;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestratorService {

    private final OrderClient orderClient;
    private final PaymentServiceClient paymentServiceClient;

    public OrderResponse executeSaga(SagaOrderRequest request) {

        log.info(
                "Saga started. idempotencyKey={}",
                request.idempotencyKey()
        );

        CreateOrderRequest orderRequest =
                new CreateOrderRequest(
                        request.userId(),
                        request.amount()
                );

        OrderResponse order =
                orderClient.createOrder(orderRequest);

        log.info(
                "Order created. orderId={}, status={}",
                order.orderId(),
                order.status()
        );

        try {

            PaymentRequest paymentRequest =
                    new PaymentRequest(
                            request.userId(),
                            order.orderId(),
                            request.amount(),
                            "INR",
                            request.simulatePaymentFailure(),
                            request.idempotencyKey()
                    );

            PaymentResponse payment =
                    paymentServiceClient.createPayment(paymentRequest);

            log.info(
                    "Payment completed. paymentId={}, status={}",
                    payment.paymentId(),
                    payment.status()
            );

            if ("SUCCESS".equals(payment.status())) {

                log.info(
                        "Confirming order. orderId={}",
                        order.orderId()
                );

                return orderClient.confirmOrder(order.orderId());
            }

            log.warn(
                    "Payment failed. Cancelling order. orderId={}",
                    order.orderId()
            );

            return orderClient.cancelOrder(order.orderId());

        } catch (Exception exception) {

            log.error(
                    "Saga failed. Cancelling order. orderId={}",
                    order.orderId(),
                    exception
            );

            return orderClient.cancelOrder(order.orderId());
        }
    }
}
