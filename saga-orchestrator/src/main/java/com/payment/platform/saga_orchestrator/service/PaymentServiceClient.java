package com.payment.platform.saga_orchestrator.service;


import com.payment.platform.saga_orchestrator.client.PaymentClient;
import com.payment.platform.saga_orchestrator.dto.PaymentRequest;
import com.payment.platform.saga_orchestrator.dto.PaymentResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final PaymentClient paymentClient;

    @Retry(name = "paymentServiceRetry")
    public PaymentResponse createPayment(PaymentRequest request) {

        return paymentClient.createPayment(request);
    }
}
