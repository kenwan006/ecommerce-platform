CREATE TABLE shipment_tracking_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  shipment_id BIGINT NOT NULL,
  provider_event_id VARCHAR(255) NOT NULL UNIQUE,
  status VARCHAR(30) NOT NULL,
  location VARCHAR(255) NULL,
  description VARCHAR(500) NULL,
  occurred_at TIMESTAMP NOT NULL,
  CONSTRAINT fk_tracking_event_shipment
    FOREIGN KEY (shipment_id) REFERENCES shipments(id),
  INDEX idx_tracking_event_shipment_time (shipment_id, occurred_at)
);
