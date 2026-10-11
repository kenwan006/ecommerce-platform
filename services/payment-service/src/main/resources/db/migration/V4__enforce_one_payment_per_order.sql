ALTER TABLE payments
  ADD CONSTRAINT uk_payments_commerce_order_id UNIQUE (commerce_order_id);
