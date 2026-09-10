ALTER TABLE payments
    ADD COLUMN idempotency_key VARCHAR(100);

UPDATE payments
SET idempotency_key = CONCAT('legacy-', id)
WHERE idempotency_key IS NULL;

ALTER TABLE payments
    MODIFY COLUMN idempotency_key VARCHAR(100) NOT NULL;

CREATE UNIQUE INDEX uk_payments_idempotency_key
    ON payments(idempotency_key);