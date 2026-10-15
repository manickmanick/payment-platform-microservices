
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StripeClient stripeClient;

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

            PaymentIntent intent = stripeClient.v1()
                    .paymentIntents()
                    .create(params, options);

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

}
