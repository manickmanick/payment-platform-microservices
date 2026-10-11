
CREATE TABLE outbox_events (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               event_id CHAR(36) NOT NULL,
                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id VARCHAR(100) NOT NULL,
                               event_type VARCHAR(100) NOT NULL,
                               topic VARCHAR(255) NOT NULL,
                               message_key VARCHAR(255) NOT NULL,
                               payload JSON NOT NULL,
                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               published_at TIMESTAMP(6) NULL,

                               PRIMARY KEY (id),
                               CONSTRAINT uk_outbox_event_id UNIQUE (event_id),
                               INDEX idx_outbox_status_created (status, created_at)
);
