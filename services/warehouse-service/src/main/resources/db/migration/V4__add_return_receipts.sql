CREATE TABLE return_receipts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  commerce_return_id BIGINT NOT NULL UNIQUE,
  commerce_order_id BIGINT NOT NULL,
  disposition VARCHAR(30) NOT NULL,
  inspection_note VARCHAR(1000) NULL,
  received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
