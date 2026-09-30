UPDATE orders
SET status = 'PENDING_PAYMENT'
WHERE status = 'PENDING';

CREATE TABLE order_status_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  previous_status VARCHAR(40) NOT NULL,
  next_status VARCHAR(40) NOT NULL,
  event VARCHAR(60) NOT NULL,
  reference_id VARCHAR(255),
  occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_order_status_history_order_occurred
  ON order_status_history (order_id, occurred_at);
