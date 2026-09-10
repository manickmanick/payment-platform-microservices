package com.payment.platform.saga_orchestrator.client;

import com.payment.platform.saga_orchestrator.dto.PaymentRequest;
import com.payment.platform.saga_orchestrator.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "payment-service",
        url="${payment-service.url}"
)
public interface PaymentClient {

    @PostMapping("/payments")
    PaymentResponse createPayment(
            @RequestBody PaymentRequest request
    );

}
