package com.payment.platform.saga_orchestrator.service;


import com.payment.platform.saga_orchestrator.client.OrderClient;
import com.payment.platform.saga_orchestrator.client.PaymentClient;
import com.payment.platform.saga_orchestrator.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import io.github.resilience4j.retry.annotation.Retry;

@Service
@RequiredArgsConstructor
public class SagaOrchestratorService {

    private final OrderClient orderClient;
    private final PaymentServiceClient paymentServiceClient;


    public OrderResponse executeSaga(SagaOrderRequest request) {

        // 1. Create order
        CreateOrderRequest orderRequest =
                new CreateOrderRequest(
                        request.userId(),
                        request.amount()
                );

        OrderResponse order =
                orderClient.createOrder(orderRequest);

        try {

            // 2. Create payment
            PaymentRequest paymentRequest =
                    new PaymentRequest(
                            request.userId(),
                            order.orderId(),
                            request.amount(),
                            "INR",
                            request.simulatePaymentFailure()
                    );

            PaymentResponse payment =
                    paymentServiceClient.createPayment(paymentRequest);

            // 3. Payment result
            if ("SUCCESS".equals(payment.status())) {

                return orderClient.confirmOrder(
                        order.orderId()
                );

            }

            // 4. Compensation
            return orderClient.cancelOrder(
                    order.orderId()
            );

        } catch (Exception exception) {

            // 5. Compensation after technical failure
            return orderClient.cancelOrder(
                    order.orderId()
            );
        }
    }

}
