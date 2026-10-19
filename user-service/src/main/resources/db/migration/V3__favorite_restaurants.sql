CREATE TABLE favorite_restaurants (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_favorite_user_restaurant UNIQUE (user_id, restaurant_id)
);
CREATE INDEX idx_favorite_user ON favorite_restaurants(user_id, created_at);
