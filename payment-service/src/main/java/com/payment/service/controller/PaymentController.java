package com.payment.service.controller;

import com.payment.service.dto.PaymentRequest;
import com.payment.service.dto.PaymentResponse;
import com.payment.service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request) {

        log.info(
                "Payment request received. orderId={}, idempotencyKey={}",
                request.orderId(),
                request.idempotencyKey()
        );

        PaymentResponse response =
                paymentService.createPayment(request);

        log.info(
                "Payment completed. paymentId={}, status={}",
                response.paymentId(),
                response.status()
        );

        return ResponseEntity.ok(response);
    }
}