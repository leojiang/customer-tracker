-- V20: Rename business_type column to certificate_type
-- This migration renames the column and updates all related database objects

-- Step 1: Drop the old check constraint
ALTER TABLE customers DROP CONSTRAINT IF EXISTS check_certificate_type;

-- Step 2: Drop the old index
DROP INDEX IF EXISTS idx_customers_business_type;

-- Step 3: Rename the column
ALTER TABLE customers RENAME COLUMN business_type TO certificate_type;

-- Step 4: Recreate the check constraint with the new column name
ALTER TABLE customers ADD CONSTRAINT check_certificate_type
CHECK (certificate_type IN ('ELECTRICIAN', 'WELDER', 'EXCAVATOR'));

-- Step 5: Recreate the index with the new column name
CREATE INDEX idx_customers_certificate_type ON customers(certificate_type) WHERE deleted_at IS NULL;

-- Step 6: Add comment to document the column
COMMENT ON COLUMN customers.certificate_type IS 'Certificate type: ELECTRICIAN, WELDER, or EXCAVATOR';
