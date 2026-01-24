-- Rename location column to address
-- This migration changes the location column to address for better clarity

-- Rename the column
ALTER TABLE customers RENAME COLUMN location TO address;

-- Update column comment
COMMENT ON COLUMN customers.address IS 'Customer address';
