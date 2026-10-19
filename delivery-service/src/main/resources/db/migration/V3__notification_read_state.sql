ALTER TABLE notifications ADD COLUMN read_at TIMESTAMP NULL;
CREATE INDEX idx_notifications_unread ON notifications(user_id, read_at, created_at);
