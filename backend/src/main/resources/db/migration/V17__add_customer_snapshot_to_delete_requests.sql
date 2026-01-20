-- Add customer_name and customer_phone columns to customer_delete_requests
-- These fields store a snapshot of customer data at the time of deletion request
-- This allows us to display customer information even after the customer is soft deleted

-- Step 1: Add columns as nullable to populate existing data
ALTER TABLE customer_delete_requests
ADD COLUMN customer_name VARCHAR(255),
ADD COLUMN customer_phone VARCHAR(20);

-- Step 2: Update existing records with customer data
UPDATE customer_delete_requests cdr
SET customer_name = c.name,
    customer_phone = c.phone
FROM customers c
WHERE cdr.customer_id = c.id
  AND cdr.customer_name IS NULL;

-- Step 3: Make columns NOT NULL after data is populated
ALTER TABLE customer_delete_requests
ALTER COLUMN customer_name SET NOT NULL,
ALTER COLUMN customer_phone SET NOT NULL;
