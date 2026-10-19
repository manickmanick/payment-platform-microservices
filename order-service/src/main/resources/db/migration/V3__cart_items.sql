CREATE TABLE cart_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  menu_item_id BIGINT NOT NULL,
  restaurant_id BIGINT NOT NULL,
  item_name VARCHAR(160) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  quantity INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_cart_user_item(user_id, menu_item_id),
  INDEX idx_cart_user(user_id)
);
