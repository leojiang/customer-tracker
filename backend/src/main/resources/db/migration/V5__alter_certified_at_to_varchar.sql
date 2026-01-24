-- Refactor certified_at from TIMESTAMP to VARCHAR(20)
-- This simplifies date handling throughout the system and avoids timezone complexity
-- New format: YYYY-MM-DD (e.g., '2024-01-15')

-- Alter customers table
-- First, add a temporary column to hold the converted data
ALTER TABLE customers ADD COLUMN certified_at_new VARCHAR(20);

-- Convert existing TIMESTAMP values to YYYY-MM-DD format
UPDATE customers
SET certified_at_new = TO_CHAR(certified_at, 'YYYY-MM-DD')
WHERE certified_at IS NOT NULL;

-- Drop the old column
ALTER TABLE customers DROP COLUMN certified_at;

-- Rename the new column to the original name
ALTER TABLE customers RENAME COLUMN certified_at_new TO certified_at;

-- Alter customer_staging table
-- First, add a temporary column
ALTER TABLE customer_staging ADD COLUMN certified_at_new VARCHAR(20);

-- Convert existing TIMESTAMP values
UPDATE customer_staging
SET certified_at_new = TO_CHAR(certified_at, 'YYYY-MM-DD')
WHERE certified_at IS NOT NULL;

-- Drop the old column
ALTER TABLE customer_staging DROP COLUMN certified_at;

-- Rename the new column
ALTER TABLE customer_staging RENAME COLUMN certified_at_new TO certified_at;
