-- Add price column to customers table
-- This migration adds a price field to store monetary values for customers

ALTER TABLE customers ADD COLUMN price DECIMAL(19,2);

-- Add comment to the price column
COMMENT ON COLUMN customers.price IS 'Customer price/amount in decimal format with 2 decimal places';