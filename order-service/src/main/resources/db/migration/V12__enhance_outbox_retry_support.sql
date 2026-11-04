ALTER TABLE outbox_events
    ADD COLUMN attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMP(6) NULL,
    ADD COLUMN last_error TEXT NULL,
    ADD COLUMN claimed_at TIMESTAMP(6) NULL;

CREATE INDEX idx_outbox_status_retry
    ON outbox_events(status, next_attempt_at, created_at);