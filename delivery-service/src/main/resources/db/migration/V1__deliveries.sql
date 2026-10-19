CREATE TABLE deliveries (
  order_id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  delivery_address VARCHAR(500) NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'READY',
  driver_id BIGINT NULL,
  latitude DECIMAL(10,7) NULL,
  longitude DECIMAL(10,7) NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_deliveries_driver(driver_id,status)
);
CREATE TABLE delivery_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  item_name VARCHAR(160) NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT fk_delivery_item_delivery FOREIGN KEY(order_id) REFERENCES deliveries(order_id)
);
CREATE TABLE notifications (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(160) NOT NULL,
  message VARCHAR(500) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notifications_user(user_id,created_at)
);
