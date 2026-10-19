ALTER TABLE orders ADD COLUMN delivery_address VARCHAR(500) NULL;
CREATE TABLE order_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  menu_item_id BIGINT NOT NULL,
  item_name VARCHAR(160) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT fk_order_item_order FOREIGN KEY(order_id) REFERENCES orders(id),
  INDEX idx_order_items_order(order_id)
);
