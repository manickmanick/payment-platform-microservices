package com.order.order_service.client;

import com.order.order_service.dto.PaymentRequest;
import com.order.order_service.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;


@FeignClient(
        name = "payment-service",
        url = "${payment-service.url}"
)
public interface PaymentClient {


    @PostMapping("/payments")
    PaymentResponse createPayment(@RequestBody PaymentRequest request, @RequestHeader("Authorization") String authorization);

    @PostMapping("/payments/orders/{orderId}/refund")
    Map<String,Object> refundOrder(@PathVariable("orderId")Long orderId,@RequestHeader("Authorization")String authorization,@RequestHeader("X-Internal-Key")String internalKey);
}
