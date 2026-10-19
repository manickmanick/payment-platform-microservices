
package com.payment.service.controller;

import com.payment.service.service.PaymentService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Charge;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public StripeWebhookController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false)
            String signatureHeader) {

        log.info("Stripe webhook request received");

        // 1. Validate that Stripe supplied the signature header.
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Stripe webhook rejected: Stripe-Signature header is missing");
            return ResponseEntity.badRequest()
                    .body("Missing Stripe-Signature header");
        }

        // 2. Verify the signature before trusting the payload.
        final Event event;

        try {
            event = Webhook.constructEvent(
                    payload,
                    signatureHeader,
                    webhookSecret
            );

        } catch (SignatureVerificationException exception) {
            log.warn("Stripe webhook rejected: signature verification failed");
            return ResponseEntity.badRequest()
                    .body("Invalid Stripe webhook signature");

        } catch (IllegalArgumentException exception) {
            log.warn("Stripe webhook rejected: invalid payload. reason={}",
                    exception.getMessage());

            return ResponseEntity.badRequest()
                    .body("Invalid Stripe webhook payload");
        }

        log.info(
                "Stripe webhook signature verified. eventId={}, eventType={}, eventApiVersion={}",
                event.getId(),
                event.getType(),
                event.getApiVersion()
        );

        // 3. Process only the payment events we currently support.
        String eventType = event.getType();

        if (!"payment_intent.succeeded".equals(eventType)
                && !"payment_intent.payment_failed".equals(eventType)
                && !"charge.refunded".equals(eventType)) {

            log.info(
                    "Ignoring unsupported Stripe event. eventId={}, eventType={}",
                    event.getId(),
                    eventType
            );

            // Return 200 so Stripe does not retry an event we intentionally ignore.
            return ResponseEntity.ok("Event ignored");
        }

        // 4. Deserialize the PaymentIntent from the verified event.
        EventDataObjectDeserializer deserializer =
                event.getDataObjectDeserializer();

        StripeObject stripeObject;
        Optional<StripeObject> safeObject = deserializer.getObject();

        if (safeObject.isPresent()) {
            stripeObject = safeObject.get();

            log.debug(
                    "Stripe event deserialized safely. eventId={}",
                    event.getId()
            );

        } else {
            // This can happen when the event API version differs from
            // the API version pinned to the Stripe Java SDK.
            log.warn(
                    "Safe event deserialization unavailable. eventId={}, eventApiVersion={}. "
                            + "Attempting unsafe deserialization.",
                    event.getId(),
                    event.getApiVersion()
            );

            try {
                stripeObject = deserializer.deserializeUnsafe();

            } catch (EventDataObjectDeserializationException exception) {
                log.error(
                        "Stripe event deserialization failed. eventId={}, eventType={}",
                        event.getId(),
                        eventType,
                        exception
                );

                return ResponseEntity.badRequest()
                        .body("Unable to deserialize Stripe event");
            }
        }

        // 5. Ensure the event contains a PaymentIntent.
        if ("charge.refunded".equals(eventType)) {
            if (!(stripeObject instanceof Charge charge) || charge.getPaymentIntent()==null) return ResponseEntity.badRequest().body("Expected a refunded Charge event");
            try { paymentService.updateRefundStatus(charge.getPaymentIntent()); return ResponseEntity.ok("Refund event processed"); }
            catch (Exception exception) { log.error("Failed to update refund status for charge {}",charge.getId(),exception);return ResponseEntity.internalServerError().body("Refund processing failed"); }
        }
        if (!(stripeObject instanceof PaymentIntent intent)) {
            log.error(
                    "Unexpected Stripe event object. eventId={}, eventType={}, objectType={}",
                    event.getId(),
                    eventType,
                    stripeObject == null
                            ? "null"
                            : stripeObject.getClass().getSimpleName()
            );

            return ResponseEntity.badRequest()
                    .body("Expected a PaymentIntent event object");
        }

        log.info(
                "Processing Stripe payment event. eventId={}, paymentIntentId={}, eventType={}",
                event.getId(),
                intent.getId(),
                eventType
        );

        // 6. Update the matching payment in MySQL.
        try {
            paymentService.updatePaymentStatus(
                    intent.getId(),
                    eventType
            );

            log.info(
                    "Stripe payment status update completed. eventId={}, paymentIntentId={}, eventType={}",
                    event.getId(),
                    intent.getId(),
                    eventType
            );

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception exception) {
            // Return 500 for processing failures so Stripe can retry delivery.
            log.error(
                    "Failed to process Stripe webhook. eventId={}, paymentIntentId={}, eventType={}",
                    event.getId(),
                    intent.getId(),
                    eventType,
                    exception
            );

            return ResponseEntity.internalServerError()
                    .body("Webhook processing failed");
        }
    }
}
