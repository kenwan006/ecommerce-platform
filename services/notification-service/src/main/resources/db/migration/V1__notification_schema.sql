CREATE TABLE notification_deliveries (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  provider_event_id VARCHAR(255) NOT NULL UNIQUE,
  event_type VARCHAR(100) NOT NULL,
  commerce_order_id BIGINT NOT NULL,
  recipient VARCHAR(255) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at TIMESTAMP NULL
);

CREATE TABLE document_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  commerce_order_id BIGINT NOT NULL,
  document_type VARCHAR(50) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  content LONGBLOB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_document_order_type (commerce_order_id, document_type)
);
