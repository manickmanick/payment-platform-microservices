CREATE TABLE coupons (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    discount_type VARCHAR(16) NOT NULL,
    discount_value DECIMAL(12,2) NOT NULL,
    max_discount DECIMAL(12,2),
    min_order_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    usage_limit INT,
    usage_count INT NOT NULL DEFAULT 0,
    starts_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_coupon_type CHECK (discount_type IN ('PERCENT','FIXED')),
    CONSTRAINT chk_coupon_value CHECK (discount_value > 0)
);

CREATE TABLE coupon_redemptions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL UNIQUE,
    redeemed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_redemption_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT uk_coupon_user UNIQUE (coupon_id, user_id)
);

ALTER TABLE orders ADD COLUMN subtotal DECIMAL(19,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(19,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN delivery_fee DECIMAL(19,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN tax_amount DECIMAL(19,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(40);
