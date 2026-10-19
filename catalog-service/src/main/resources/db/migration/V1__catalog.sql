CREATE TABLE categories (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  image_url VARCHAR(1000)
);
CREATE TABLE restaurants (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(160) NOT NULL,
  description VARCHAR(1000),
  address VARCHAR(500) NOT NULL,
  city VARCHAR(100) NOT NULL,
  phone_number VARCHAR(30),
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  image_url VARCHAR(1000),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE menu_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  restaurant_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  name VARCHAR(160) NOT NULL,
  description VARCHAR(1000),
  price DECIMAL(12,2) NOT NULL,
  food_type VARCHAR(20) NOT NULL DEFAULT 'VEG',
  available BOOLEAN NOT NULL DEFAULT TRUE,
  image_url VARCHAR(1000),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_menu_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants(id),
  CONSTRAINT fk_menu_category FOREIGN KEY (category_id) REFERENCES categories(id),
  INDEX idx_menu_restaurant (restaurant_id)
);
INSERT INTO categories(name) VALUES ('Pizza'),('Burgers'),('Indian'),('Chinese'),('Desserts'),('Beverages');
