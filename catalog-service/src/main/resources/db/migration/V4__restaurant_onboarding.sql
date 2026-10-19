ALTER TABLE restaurants ADD COLUMN approval_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
CREATE INDEX idx_restaurants_approval_status ON restaurants(approval_status, status, city);
