-- One-time local Commerce data migration.
--
-- Run this only after Commerce has started once against sunridge_commerce,
-- because Flyway creates the target tables. It does not modify ecommerce.
-- The statements are idempotent: they preserve the existing IDs and update
-- a target row if the script is run again.

INSERT INTO sunridge_commerce.users (
  id, name, email, password_hash, role, created_at,
  oauth_provider, oauth_provider_subject
)
SELECT
  id, name, email, password_hash, role, created_at,
  oauth_provider, oauth_provider_subject
FROM ecommerce.users
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  email = VALUES(email),
  password_hash = VALUES(password_hash),
  role = VALUES(role),
  created_at = VALUES(created_at),
  oauth_provider = VALUES(oauth_provider),
  oauth_provider_subject = VALUES(oauth_provider_subject);

INSERT INTO sunridge_commerce.products (
  id, name, description, price, stock, image_url
)
SELECT id, name, description, price, stock, image_url
FROM ecommerce.products
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  price = VALUES(price),
  stock = VALUES(stock),
  image_url = VALUES(image_url);

INSERT INTO sunridge_commerce.orders (
  id, user_id, total, status, stripe_payment_intent_id, checkout_id,
  payment_status, stock_reserved, created_at
)
SELECT
  id, user_id, total, status, stripe_payment_intent_id, checkout_id,
  payment_status, stock_reserved, created_at
FROM ecommerce.orders
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  total = VALUES(total),
  status = VALUES(status),
  stripe_payment_intent_id = VALUES(stripe_payment_intent_id),
  checkout_id = VALUES(checkout_id),
  payment_status = VALUES(payment_status),
  stock_reserved = VALUES(stock_reserved),
  created_at = VALUES(created_at);

INSERT INTO sunridge_commerce.order_items (
  id, order_id, product_id, quantity, unit_price
)
SELECT id, order_id, product_id, quantity, unit_price
FROM ecommerce.order_items
ON DUPLICATE KEY UPDATE
  order_id = VALUES(order_id),
  product_id = VALUES(product_id),
  quantity = VALUES(quantity),
  unit_price = VALUES(unit_price);

INSERT INTO sunridge_commerce.fraud_assessments (
  id, order_id, decision, risk_score, reasons, model_version, created_at
)
SELECT id, order_id, decision, risk_score, reasons, model_version, created_at
FROM ecommerce.fraud_assessments
ON DUPLICATE KEY UPDATE
  order_id = VALUES(order_id),
  decision = VALUES(decision),
  risk_score = VALUES(risk_score),
  reasons = VALUES(reasons),
  model_version = VALUES(model_version),
  created_at = VALUES(created_at);
