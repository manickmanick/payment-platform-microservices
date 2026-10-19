ALTER TABLE order_items ADD COLUMN restaurant_id BIGINT NULL;
UPDATE order_items oi JOIN cart_items ci ON ci.menu_item_id=oi.menu_item_id SET oi.restaurant_id=ci.restaurant_id;
UPDATE order_items SET restaurant_id=0 WHERE restaurant_id IS NULL;
ALTER TABLE order_items MODIFY restaurant_id BIGINT NOT NULL;
