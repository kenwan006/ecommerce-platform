CREATE TABLE fraud_assessments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL UNIQUE,
  decision VARCHAR(20) NOT NULL,
  risk_score DECIMAL(6,5) NOT NULL,
  reasons VARCHAR(1000) NOT NULL,
  model_version VARCHAR(100) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_fraud_assessment_order FOREIGN KEY (order_id) REFERENCES orders(id)
);
