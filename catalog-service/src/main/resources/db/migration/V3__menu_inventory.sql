ALTER TABLE menu_items
    ADD COLUMN stock_quantity INT NOT NULL DEFAULT 100,
    ADD COLUMN average_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0;

ALTER TABLE restaurants
    ADD COLUMN average_rating DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0;

CREATE TABLE restaurant_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    restaurant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE,
    CONSTRAINT uk_reviews_restaurant_user UNIQUE (restaurant_id, user_id),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_restaurant_reviews_created ON restaurant_reviews(restaurant_id, created_at);
