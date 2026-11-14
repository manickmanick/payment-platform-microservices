
package com.payment.service.service;

import com.payment.service.dto.PaymentRequest;
import com.payment.service.dto.PaymentResponse;
import com.payment.service.entity.OutboxEvent;
import com.payment.service.entity.Payment;
import com.payment.service.entity.PaymentStatus;
import com.payment.service.repository.OutboxEventRepository;
import com.payment.service.repository.PaymentRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.MDC;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StripeClient stripeClient;
    private final StripePaymentIntentService stripePaymentIntentService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;


    // ---------------------------------------------------------
    // 1. CREATE PAYMENT
    // ---------------------------------------------------------

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {

        return paymentRepository
                .findByIdempotencyKey(request.idempotencyKey())
                .map(this::toResponse)
                .orElseGet(() -> createNewPayment(request));
    }


    private PaymentResponse createNewPayment(PaymentRequest request) {

        try {

            String correlationId = MDC.get("correlationId");

            long amountInPaise = request.amount()
                    .movePointRight(2)
                    .longValueExact();

            PaymentIntentCreateParams params =
                    PaymentIntentCreateParams.builder()
                            .setAmount(amountInPaise)
                            .setCurrency(
                                    request.currency()
                                            .toLowerCase(Locale.ROOT)
                            )
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams
                                            .AutomaticPaymentMethods
                                            .builder()
                                            .setEnabled(true)
                                            .build()
                            )
                            .putMetadata(
                                    "orderId",
                                    request.orderId().toString()
                            )
                            .putMetadata(
                                    "idempotencyKey",
                                    request.idempotencyKey()
                            )
                            .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(request.idempotencyKey())
                    .build();

            PaymentIntent intent = stripePaymentIntentService
                    .createPaymentIntent(params, options);

            Payment payment = Payment.builder()
                    .userId(request.userId())
                    .orderId(request.orderId())
                    .amount(request.amount())
                    .currency(request.currency())
                    .status(PaymentStatus.PENDING)
                    .idempotencyKey(request.idempotencyKey())
                    .stripePaymentIntentId(intent.getId())
                    .correlationId(correlationId)
                    .createdAt(LocalDateTime.now())
                    .build();

            Payment savedPayment = paymentRepository.save(payment);

            return toResponse(savedPayment);

        } catch (StripeException exception) {

            throw new IllegalStateException(
                    "Failed to create Stripe PaymentIntent",
                    exception
            );
        }
    }


    private PaymentResponse toResponse(Payment payment) {

        String clientSecret = null;

        if (payment.getStripePaymentIntentId() != null) {

            try {

                PaymentIntent intent = stripeClient.v1()
                        .paymentIntents()
                        .retrieve(payment.getStripePaymentIntentId());

                clientSecret = intent.getClientSecret();

            } catch (StripeException exception) {

                throw new IllegalStateException(
                        "Failed to retrieve Stripe PaymentIntent",
                        exception
                );
            }
        }

        return new PaymentResponse(
                payment.getId(),
                payment.getStatus().name(),
                clientSecret
        );
    }


    // ---------------------------------------------------------
    // 2. UPDATE PAYMENT STATUS - STRIPE WEBHOOK
    // ---------------------------------------------------------

    @Transactional
    public void updatePaymentStatus(
            String stripePaymentIntentId,
            String stripeEventType
    ) {

        /*
         * PESSIMISTIC WRITE LOCK
         *
         * Concurrent webhook requests for the same PaymentIntent
         * must wait until the first transaction completes.
         */

        Payment payment = paymentRepository
                .findByStripePaymentIntentIdForUpdate(
                        stripePaymentIntentId
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found for Stripe PaymentIntent: "
                                + stripePaymentIntentId
                ));

        PaymentStatus newStatus;

        if ("payment_intent.succeeded".equals(stripeEventType)) {

            newStatus = PaymentStatus.SUCCESS;

        } else if ("payment_intent.payment_failed".equals(stripeEventType)) {

            newStatus = PaymentStatus.FAILED;

        } else {

            throw new IllegalArgumentException(
                    "Unsupported Stripe event: " + stripeEventType
            );
        }


        // Duplicate event protection

        if (payment.getStatus() == newStatus) {

            return;
        }


        // Never overwrite a successful or refunded payment.

        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.REFUNDED) {

            return;
        }


        // Update payment status

        payment.setStatus(newStatus);
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);


        // Create Outbox event only for successful payment.

        if (newStatus == PaymentStatus.SUCCESS) {

            createPaymentSucceededOutboxEvent(payment);
        }
    }


    // ---------------------------------------------------------
    // 3. GET CLIENT SECRET
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public String getClientSecret(
            Long paymentId,
            Long requestingUserId
    ) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found: " + paymentId
                ));

        if (!payment.getUserId().equals(requestingUserId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Payment belongs to another user"
            );
        }

        if (payment.getStripePaymentIntentId() == null) {

            throw new IllegalStateException(
                    "No Stripe PaymentIntent exists for payment: "
                            + paymentId
            );
        }

        try {

            PaymentIntent intent = stripeClient.v1()
                    .paymentIntents()
                    .retrieve(payment.getStripePaymentIntentId());

            return intent.getClientSecret();

        } catch (StripeException exception) {

            throw new IllegalStateException(
                    "Could not retrieve Stripe client secret",
                    exception
            );
        }
    }


    // ---------------------------------------------------------
    // 4. LIST USER PAYMENTS
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPayments(Long userId) {

        return paymentRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::summary)
                .toList();
    }


    // ---------------------------------------------------------
    // 5. GET SINGLE PAYMENT
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public Map<String, Object> getPayment(
            Long paymentId,
            Long userId
    ) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found: " + paymentId
                ));

        if (!payment.getUserId().equals(userId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Payment belongs to another user"
            );
        }

        return summary(payment);
    }


    private Map<String, Object> summary(Payment payment) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("paymentId", payment.getId());
        response.put("orderId", payment.getOrderId());
        response.put("amount", payment.getAmount());
        response.put("currency", payment.getCurrency());
        response.put("status", payment.getStatus().name());
        response.put("createdAt", payment.getCreatedAt());
        response.put("updatedAt", payment.getUpdatedAt());

        return response;
    }


    // ---------------------------------------------------------
    // 6. REFUND ORDER
    // ---------------------------------------------------------

    @Transactional
    public Map<String, Object> refundOrder(
            Long orderId,
            Long requestingUserId
    ) {

        Payment payment = paymentRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .filter(p -> p.getUserId().equals(requestingUserId))
                .filter(p ->
                        p.getStatus() == PaymentStatus.SUCCESS
                                || p.getStatus() == PaymentStatus.REFUNDED
                )
                .findFirst()
                .orElse(null);


        if (payment == null) {

            boolean ownsAttempt = paymentRepository
                    .findByOrderId(orderId)
                    .stream()
                    .anyMatch(p ->
                            p.getUserId().equals(requestingUserId)
                    );

            if (!ownsAttempt) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found for order"
                );
            }

            return Map.of(
                    "status", "NO_CAPTURED_PAYMENT",
                    "orderId", orderId
            );
        }


        if (payment.getStatus() == PaymentStatus.REFUNDED) {

            return Map.of(
                    "status", "REFUNDED",
                    "orderId", orderId
            );
        }


        try {

            Refund refund = stripeClient.v1()
                    .refunds()
                    .create(
                            RefundCreateParams.builder()
                                    .setPaymentIntent(
                                            payment.getStripePaymentIntentId()
                                    )
                                    .build(),
                            RequestOptions.builder()
                                    .setIdempotencyKey(
                                            "refund-order-" + orderId
                                    )
                                    .build()
                    );


            if ("succeeded".equals(refund.getStatus())) {

                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setUpdatedAt(LocalDateTime.now());

                paymentRepository.save(payment);
            }


            return Map.of(
                    "status",
                    Objects.toString(refund.getStatus(), "PENDING"),
                    "refundId",
                    Objects.toString(refund.getId(), ""),
                    "orderId",
                    orderId
            );

        } catch (StripeException exception) {

            throw new IllegalStateException(
                    "Stripe refund failed",
                    exception
            );
        }
    }


    // ---------------------------------------------------------
    // 7. UPDATE REFUND STATUS - STRIPE WEBHOOK
    // ---------------------------------------------------------

    @Transactional
    public void updateRefundStatus(String stripePaymentIntentId) {

        Payment payment = paymentRepository
                .findByStripePaymentIntentIdForUpdate(
                        stripePaymentIntentId
                )
                .orElse(null);

        if (payment == null) {
            return;
        }

        if (payment.getStatus() != PaymentStatus.REFUNDED) {

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());

            paymentRepository.save(payment);
        }
    }


    // ---------------------------------------------------------
    // 8. RESUME PAYMENT FOR ORDER
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public Map<String, Object> resumePaymentForOrder(
            Long orderId,
            Long requestingUserId
    ) {

        Payment payment = paymentRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .filter(p -> p.getUserId().equals(requestingUserId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found for this order"
                ));


        if (payment.getStripePaymentIntentId() == null) {

            throw new IllegalStateException(
                    "Stripe PaymentIntent is not available"
            );
        }


        try {

            PaymentIntent intent = stripeClient.v1()
                    .paymentIntents()
                    .retrieve(payment.getStripePaymentIntentId());

            String stripeStatus = intent.getStatus();


            /*
             * Stripe confirms payment success.
             *
             * Do not update Payment DB here.
             * Stripe Webhook is responsible for updating Payment
             * and creating the Outbox event.
             */

            if ("succeeded".equals(stripeStatus)) {

                return Map.of(
                        "status", "ALREADY_PAID",
                        "paymentStatus", payment.getStatus().name(),
                        "orderId", orderId
                );
            }


            if ("canceled".equals(stripeStatus)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This PaymentIntent was canceled. "
                                + "A new payment attempt is required."
                );
            }


            if (!"requires_payment_method".equals(stripeStatus)
                    && !"requires_confirmation".equals(stripeStatus)
                    && !"requires_action".equals(stripeStatus)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Payment cannot be resumed. Current Stripe status: "
                                + stripeStatus
                );
            }


            Map<String, Object> response = new LinkedHashMap<>();

            response.put("paymentId", payment.getId());
            response.put("orderId", payment.getOrderId());
            response.put("amount", payment.getAmount());
            response.put("currency", payment.getCurrency());
            response.put("status", payment.getStatus().name());
            response.put("clientSecret", intent.getClientSecret());

            return response;

        } catch (StripeException exception) {

            throw new IllegalStateException(
                    "Could not retrieve Stripe PaymentIntent",
                    exception
            );
        }
    }


    // ---------------------------------------------------------
    // 9. CREATE PAYMENT SUCCEEDED OUTBOX EVENT
    // ---------------------------------------------------------

    private void createPaymentSucceededOutboxEvent(Payment payment) {

        try {

            String eventId = UUID.randomUUID().toString();

            Map<String, Object> payload = Map.of(
                    "eventId", eventId,
                    "orderId", payment.getOrderId(),
                    "paymentId", payment.getId(),
                    "userId", payment.getUserId(),
                    "amount", payment.getAmount(),
                    "currency", payment.getCurrency()
            );


            String payloadJson = objectMapper.writeValueAsString(payload);


            OutboxEvent outboxEvent = new OutboxEvent();

            outboxEvent.setEventId(eventId);
            outboxEvent.setAggregateType("PAYMENT");
            outboxEvent.setAggregateId(payment.getId().toString());
            outboxEvent.setEventType("PaymentSucceededEvent");
            outboxEvent.setTopic("payment-succeeded");
            outboxEvent.setMessageKey(payment.getOrderId().toString());
            outboxEvent.setPayload(payloadJson);
            outboxEvent.setStatus("PENDING");
            outboxEvent.setCorrelationId(payment.getCorrelationId());
            outboxEvent.setAttempts(0);


            outboxEventRepository.save(outboxEvent);

        } catch (Exception exception) {

            /*
             * Throwing this exception causes the transaction
             * to roll back, including the Payment status update.
             */

            throw new IllegalStateException(
                    "Failed to create PaymentSucceeded Outbox event",
                    exception
            );
        }
    }


    // ---------------------------------------------------------
    // 10. VERIFY PAYMENT - READ ONLY
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public String verifyPaymentIntent(
            String stripePaymentIntentId,
            Long requestingUserId
    ) {

        Payment payment = paymentRepository
                .findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found"
                ));


        if (!payment.getUserId().equals(requestingUserId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Payment belongs to another user"
            );
        }


        try {

            PaymentIntent intent = stripeClient.v1()
                    .paymentIntents()
                    .retrieve(stripePaymentIntentId);


            if ("succeeded".equals(intent.getStatus())) {

                return payment.getStatus() == PaymentStatus.SUCCESS
                        ? "SUCCESS"
                        : "PROCESSING";
            }


            if ("canceled".equals(intent.getStatus())) {

                return "FAILED";
            }


            return payment.getStatus().name();

        } catch (StripeException exception) {

            throw new IllegalStateException(
                    "Could not verify Stripe payment",
                    exception
            );
        }
    }
}
