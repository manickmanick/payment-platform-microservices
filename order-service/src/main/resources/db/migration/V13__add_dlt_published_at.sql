ALTER TABLE outbox_events
    ADD COLUMN dlt_published_at DATETIME(6) NULL;