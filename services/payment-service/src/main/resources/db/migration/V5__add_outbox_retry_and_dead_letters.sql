ALTER TABLE outbox_events
  ADD COLUMN retry_count INT NOT NULL DEFAULT 0,
  ADD COLUMN last_error TEXT NULL,
  ADD COLUMN next_attempt_at TIMESTAMP NULL,
  ADD COLUMN dead_lettered_at TIMESTAMP NULL,
  ADD INDEX idx_outbox_ready (published_at, dead_lettered_at, next_attempt_at, occurred_at);

CREATE TABLE outbox_dead_letters (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  outbox_event_id CHAR(36) NOT NULL UNIQUE,
  event_type VARCHAR(100) NOT NULL,
  aggregate_id VARCHAR(100) NOT NULL,
  payload JSON NOT NULL,
  last_error TEXT NOT NULL,
  retry_count INT NOT NULL,
  dead_lettered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
