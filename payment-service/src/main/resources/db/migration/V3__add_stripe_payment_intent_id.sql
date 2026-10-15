ALTER TABLE payments
    ADD COLUMN stripe_payment_intent_id VARCHAR(255) NULL;

CREATE UNIQUE INDEX uk_payments_stripe_payment_intent_id
    ON payments(stripe_payment_intent_id);