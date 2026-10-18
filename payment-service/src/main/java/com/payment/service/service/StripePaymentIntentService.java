package com.payment.service.service;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class StripePaymentIntentService {

    private final StripeClient stripeClient;

    private final AtomicInteger attemptCounter = new AtomicInteger(0);

    @Retry(name = "stripePaymentIntent")
    public PaymentIntent createPaymentIntent(
            PaymentIntentCreateParams params,
            RequestOptions options
    ) throws StripeException {

        int attempt = attemptCounter.incrementAndGet();

        System.out.println("Stripe attempt: " + attempt);

        if (attempt <= 2) {
            throw new IllegalStateException(
                    "Simulated temporary failure for retry testing"
            );
        }

        return stripeClient.v1()
                .paymentIntents()
                .create(params, options);
    }
}