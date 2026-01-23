-- Convert customer_status enum to VARCHAR for better JPA compatibility

-- First, add new VARCHAR columns
ALTER TABLE customers ADD COLUMN current_status_new VARCHAR(50);
ALTER TABLE status_history ADD COLUMN from_status_new VARCHAR(50);
ALTER TABLE status_history ADD COLUMN to_status_new VARCHAR(50);

-- Copy data from enum columns to VARCHAR columns
UPDATE customers SET current_status_new = current_status::text;
UPDATE status_history SET from_status_new = from_status::text;
UPDATE status_history SET to_status_new = to_status::text;

-- Drop old enum columns
ALTER TABLE customers DROP COLUMN current_status;
ALTER TABLE status_history DROP COLUMN from_status;
ALTER TABLE status_history DROP COLUMN to_status;

-- Rename new columns to original names
ALTER TABLE customers RENAME COLUMN current_status_new TO current_status;
ALTER TABLE status_history RENAME COLUMN from_status_new TO from_status;
ALTER TABLE status_history RENAME COLUMN to_status_new TO to_status;

-- Add NOT NULL constraint to required columns
ALTER TABLE customers ALTER COLUMN current_status SET NOT NULL;
ALTER TABLE status_history ALTER COLUMN to_status SET NOT NULL;

-- Set default value
ALTER TABLE customers ALTER COLUMN current_status SET DEFAULT 'Customer called';

-- Drop the enum type
DROP TYPE IF EXISTS customer_status;