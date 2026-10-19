ALTER TABLE deliveries ADD COLUMN delivery_latitude DECIMAL(10,7) NULL;
ALTER TABLE deliveries ADD COLUMN delivery_longitude DECIMAL(10,7) NULL;

CREATE TABLE driver_presence (
    user_id BIGINT NOT NULL PRIMARY KEY,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE INDEX idx_driver_presence_available ON driver_presence(available, updated_at);
