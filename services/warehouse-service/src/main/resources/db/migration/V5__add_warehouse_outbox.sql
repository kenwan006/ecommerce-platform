CREATE TABLE warehouse_outbox_events (
  id CHAR(36) PRIMARY KEY,
  aggregate_type VARCHAR(100) NOT NULL,
  aggregate_id VARCHAR(100) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  payload JSON NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  published_at TIMESTAMP NULL,
  INDEX idx_warehouse_outbox_unpublished (published_at, occurred_at)
);
