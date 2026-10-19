CREATE TABLE order_event_receipts (
    event_id VARCHAR(100) NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    received_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_order_event_receipts_order_id ON order_event_receipts(order_id);
