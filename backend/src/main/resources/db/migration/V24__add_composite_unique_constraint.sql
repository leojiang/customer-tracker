-- Add composite unique constraint on (phone, certificate_type) to support multiple certificates per phone number
-- This allows one person to have multiple different certificate types, but prevents duplicate certificate types for the same phone

-- Step 1: Drop the existing unique constraint on phone
ALTER TABLE customers DROP CONSTRAINT IF EXISTS unique_phone;

-- Step 2: Add the new composite unique constraint on (phone, certificate_type)
-- This allows multiple records with the same phone number as long as they have different certificate types
ALTER TABLE customers ADD CONSTRAINT unique_phone_certificate_type UNIQUE (phone, certificate_type);

-- Step 3: Create index for better performance on queries filtering by phone
CREATE INDEX IF NOT EXISTS idx_customers_phone_certificate ON customers(phone, certificate_type) WHERE deleted_at IS NULL;