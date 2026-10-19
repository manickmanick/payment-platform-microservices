
package com.payment.service.service;

import com.payment.service.dto.PaymentRequest;
import com.payment.service.dto.PaymentResponse;
import com.payment.service.entity.Payment;
import com.payment.service.entity.PaymentStatus;
import com.payment.service.repository.PaymentRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.model.Refund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.ApiException;
import com.stripe.exception.RateLimitException;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StripeClient stripeClient;
    private final StripePaymentIntentService stripePaymentIntentService;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        return paymentRepository
                .findByIdempotencyKey(request.idempotencyKey())
                .map(this::toResponse)
                .orElseGet(() -> createNewPayment(request));
    }


    private PaymentResponse createNewPayment(PaymentRequest request) {
        try {
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


    @Transactional
    public void updatePaymentStatus(
            String stripePaymentIntentId,
            String stripeEventType
    ) {
        Payment payment = paymentRepository
                .findByStripePaymentIntentId(stripePaymentIntentId)
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

        // Avoid unnecessary updates when Stripe retries the same event.
        if (payment.getStatus() == newStatus) {
            return;
        }

        // Do not overwrite a terminal status with a conflicting event.
        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            return;
        }

        payment.setStatus(newStatus);
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);
    }


    public String getClientSecret(Long paymentId, Long requestingUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found: " + paymentId
                ));
        if (!payment.getUserId().equals(requestingUserId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Payment belongs to another user");

        if (payment.getStripePaymentIntentId() == null) {
            throw new IllegalStateException(
                    "No Stripe PaymentIntent exists for payment: " + paymentId
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

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public java.util.List<Map<String,Object>> listPayments(Long userId){return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::summary).toList();}

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public Map<String,Object> getPayment(Long paymentId,Long userId){Payment p=paymentRepository.findById(paymentId).orElseThrow(()->new IllegalArgumentException("Payment not found: "+paymentId));if(!p.getUserId().equals(userId))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Payment belongs to another user");return summary(p);}

    private Map<String,Object> summary(Payment p){Map<String,Object> m=new java.util.LinkedHashMap<>();m.put("paymentId",p.getId());m.put("orderId",p.getOrderId());m.put("amount",p.getAmount());m.put("currency",p.getCurrency());m.put("status",p.getStatus().name());m.put("createdAt",p.getCreatedAt());m.put("updatedAt",p.getUpdatedAt());return m;}

    @Transactional
    public String verifyPaymentIntent(String stripePaymentIntentId, Long requestingUserId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (!payment.getUserId().equals(requestingUserId)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Payment belongs to another user");
        try {
            PaymentIntent intent = stripeClient.v1().paymentIntents().retrieve(stripePaymentIntentId);
            if ("succeeded".equals(intent.getStatus())) {
                updatePaymentStatus(stripePaymentIntentId, "payment_intent.succeeded");
                return "SUCCESS";
            }
            if ("canceled".equals(intent.getStatus())) {
                updatePaymentStatus(stripePaymentIntentId, "payment_intent.payment_failed");
                return "FAILED";
            }
            return payment.getStatus().name();
        } catch (StripeException exception) {
            throw new IllegalStateException("Could not verify Stripe payment", exception);
        }
    }

    @Transactional
    public Map<String,Object> refundOrder(Long orderId,Long requestingUserId) {
        Payment payment=paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .filter(p->p.getUserId().equals(requestingUserId))
                .filter(p->p.getStatus()==PaymentStatus.SUCCESS||p.getStatus()==PaymentStatus.REFUNDED)
                .findFirst().orElse(null);
        if(payment==null){boolean ownsAttempt=paymentRepository.findByOrderId(orderId).stream().anyMatch(p->p.getUserId().equals(requestingUserId));if(!ownsAttempt)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Payment not found for order");return Map.of("status","NO_CAPTURED_PAYMENT","orderId",orderId);}
        if(payment.getStatus()==PaymentStatus.REFUNDED)return Map.of("status","REFUNDED","orderId",orderId);
        try {
            Refund refund=stripeClient.v1().refunds().create(RefundCreateParams.builder().setPaymentIntent(payment.getStripePaymentIntentId()).build(),RequestOptions.builder().setIdempotencyKey("refund-order-"+orderId).build());
            if("succeeded".equals(refund.getStatus())){payment.setStatus(PaymentStatus.REFUNDED);payment.setUpdatedAt(LocalDateTime.now());paymentRepository.save(payment);}
            return Map.of("status",Objects.toString(refund.getStatus(),"PENDING"),"refundId",Objects.toString(refund.getId(),""),"orderId",orderId);
        } catch(StripeException exception){throw new IllegalStateException("Stripe refund failed",exception);}
    }

    @Transactional public void updateRefundStatus(String stripePaymentIntentId){paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId).ifPresent(p->{if(p.getStatus()!=PaymentStatus.REFUNDED){p.setStatus(PaymentStatus.REFUNDED);p.setUpdatedAt(LocalDateTime.now());paymentRepository.save(p);}});}


}
