ALTER TABLE products ADD COLUMN sku VARCHAR(64) NULL;

UPDATE products
SET sku = CASE name
  WHEN 'Everyday Tee' THEN 'TEE-001'
  WHEN 'Canvas Tote' THEN 'TOTE-001'
  WHEN 'Ceramic Mug' THEN 'MUG-001'
  WHEN 'Merino Beanie' THEN 'BEANIE-001'
  WHEN 'Field Notebook' THEN 'NOTEBOOK-001'
  WHEN 'Insulated Bottle' THEN 'BOTTLE-001'
  WHEN 'Everyday Cap' THEN 'CAP-001'
  WHEN 'Wool Throw' THEN 'THROW-001'
  WHEN 'Desk Tray' THEN 'TRAY-001'
  ELSE CONCAT('SKU-', id)
END
WHERE sku IS NULL;

ALTER TABLE products MODIFY COLUMN sku VARCHAR(64) NOT NULL;
ALTER TABLE products ADD CONSTRAINT uk_products_sku UNIQUE (sku);
