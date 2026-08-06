ALTER TABLE orders
  ADD COLUMN checkout_id VARCHAR(255) NULL;

UPDATE orders
  SET checkout_id = CONCAT('legacy-', id)
  WHERE checkout_id IS NULL;

ALTER TABLE orders
  MODIFY COLUMN checkout_id VARCHAR(255) NOT NULL,
  ADD CONSTRAINT uk_orders_user_checkout_id UNIQUE (user_id, checkout_id);
