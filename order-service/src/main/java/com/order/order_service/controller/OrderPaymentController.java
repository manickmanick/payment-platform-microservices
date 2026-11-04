package com.order.order_service.controller;

import com.order.order_service.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderPaymentController {

    private final CheckoutService checkoutService;

    @PostMapping("/{orderId}/payment-session")
    public ResponseEntity<Map<String, Object>> resumePayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long orderId
    ) {

        Long userId = Long.valueOf(
                jwt.getClaim("userId").toString()
        );

        String token = jwt.getTokenValue();

        Map<String, Object> response =
                checkoutService.resumePayment(
                        orderId,
                        userId,
                        token
                );

        return ResponseEntity.ok(response);
    }
}