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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    @Value("${payment.internal-key:local-payment-key-change-me}") private String paymentInternalKey;

    @GetMapping
    public ResponseEntity<java.util.Map<String,Object>> listPayments(@AuthenticationPrincipal Jwt jwt){var rows=paymentService.listPayments(Long.valueOf(jwt.getClaim("userId").toString()));return ResponseEntity.ok(java.util.Map.of("content",rows,"totalElements",rows.size()));}

    @PostMapping("/orders/{orderId}/refund")
    public ResponseEntity<Map<String,Object>> refundOrder(@AuthenticationPrincipal Jwt jwt,@PathVariable Long orderId,@org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key")String provided){if(!MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8),paymentInternalKey.getBytes(StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid internal service credential");return ResponseEntity.ok(paymentService.refundOrder(orderId,Long.valueOf(jwt.getClaim("userId").toString())));}

    @GetMapping("/{paymentId}")
    public ResponseEntity<Map<String,Object>> getPayment(@AuthenticationPrincipal Jwt jwt,@PathVariable Long paymentId){return ResponseEntity.ok(paymentService.getPayment(paymentId,Long.valueOf(jwt.getClaim("userId").toString())));}

    @PostMapping("/verify")
    public ResponseEntity<Map<String,String>> verify(@AuthenticationPrincipal Jwt jwt,@RequestBody Map<String,String> body) {
        String intentId=body.get("paymentIntentId");
        if(intentId==null || intentId.isBlank()) return ResponseEntity.badRequest().body(Map.of("message","paymentIntentId is required"));
        String status=paymentService.verifyPaymentIntent(intentId,Long.valueOf(jwt.getClaim("userId").toString()));
        if(!"SUCCESS".equals(status)) return ResponseEntity.status(409).body(Map.of("message","Payment is not successful yet","status",status));
        return ResponseEntity.ok(Map.of("status",status));
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PaymentRequest request) {
        if(!request.userId().equals(Long.valueOf(jwt.getClaim("userId").toString()))) return ResponseEntity.status(403).build();

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


    @GetMapping("/{paymentId}/client-secret")
    public ResponseEntity<Map<String, String>> getClientSecret(
            @AuthenticationPrincipal Jwt jwt,@PathVariable Long paymentId
    ) {
        String clientSecret = paymentService.getClientSecret(paymentId,Long.valueOf(jwt.getClaim("userId").toString()));

        return ResponseEntity.ok(
                Map.of("clientSecret", clientSecret)
        );
    }


}
