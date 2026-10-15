
package com.payment.service.controller;

import com.payment.service.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature
    ) {
        Event event;

        try {
            event = Webhook.constructEvent(
                    payload,
                    signature,
                    webhookSecret
            );
        } catch (SignatureVerificationException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid webhook signature");
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid webhook payload");
        }

        if ("payment_intent.succeeded".equals(event.getType())
                || "payment_intent.payment_failed".equals(event.getType())) {

            Optional<StripeObject> stripeObject =
                    event.getDataObjectDeserializer().getObject();

            if (stripeObject.isEmpty()
                    || !(stripeObject.get() instanceof PaymentIntent intent)) {
                return ResponseEntity.badRequest()
                        .body("Unable to deserialize PaymentIntent");
            }

            paymentService.updatePaymentStatus(
                    intent.getId(),
                    event.getType()
            );
        }

        return ResponseEntity.ok("Webhook received");
    }

}
