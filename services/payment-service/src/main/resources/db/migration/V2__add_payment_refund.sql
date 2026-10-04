ALTER TABLE payments
  ADD COLUMN provider_refund_id VARCHAR(255) NULL UNIQUE;
