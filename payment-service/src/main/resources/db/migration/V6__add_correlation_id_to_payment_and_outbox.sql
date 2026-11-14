ALTER TABLE payments
    ADD COLUMN correlation_id VARCHAR(100) NULL;


ALTER TABLE outbox_events
    ADD COLUMN correlation_id VARCHAR(100) NULL;