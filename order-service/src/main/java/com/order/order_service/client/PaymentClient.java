package com.order.order_service.client;

import com.order.order_service.dto.PaymentRequest;
import com.order.order_service.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(
        name = "payment-service",
        url = "${payment-service.url}"
)
public interface PaymentClient {


    @PostMapping("/payments")
    PaymentResponse createPayment(@RequestBody PaymentRequest request);
}
