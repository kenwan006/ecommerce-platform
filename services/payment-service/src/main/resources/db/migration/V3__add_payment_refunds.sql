CREATE TABLE payment_refunds (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  payment_id BIGINT NOT NULL,
  commerce_order_id BIGINT NOT NULL,
  commerce_return_id BIGINT NOT NULL UNIQUE,
  provider_refund_id VARCHAR(255) NOT NULL UNIQUE,
  amount DECIMAL(19, 2) NOT NULL,
  currency VARCHAR(10) NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL,
  CONSTRAINT fk_payment_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
  INDEX idx_payment_refunds_order (commerce_order_id)
);
