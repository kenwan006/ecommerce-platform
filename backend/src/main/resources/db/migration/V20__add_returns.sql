CREATE TABLE return_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  rma_number VARCHAR(64) NOT NULL UNIQUE,
  status VARCHAR(30) NOT NULL,
  reason VARCHAR(500) NULL,
  carrier VARCHAR(100) NOT NULL,
  tracking_number VARCHAR(255) NOT NULL UNIQUE,
  label_url VARCHAR(500) NOT NULL,
  inspection_note VARCHAR(1000) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  received_at TIMESTAMP NULL,
  refunded_at TIMESTAMP NULL,
  CONSTRAINT uk_return_requests_order UNIQUE (order_id),
  CONSTRAINT fk_return_requests_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE return_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  return_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(255) NOT NULL,
  quantity INT NOT NULL,
  CONSTRAINT fk_return_items_return FOREIGN KEY (return_id) REFERENCES return_requests(id),
  CONSTRAINT fk_return_items_product FOREIGN KEY (product_id) REFERENCES products(id)
);
