-- Add customer_agent column to customers table
ALTER TABLE customers ADD COLUMN IF NOT EXISTS customer_agent VARCHAR(255);

-- Add comment
COMMENT ON COLUMN customers.customer_agent IS 'The agent who introduced this customer into the business';
