-- Rename price column to id_card
-- This migration changes the price DECIMAL column to id_card VARCHAR column
-- to store identity card numbers instead of price values

-- Step 1: Add the new id_card column
ALTER TABLE customers ADD COLUMN id_card VARCHAR(255);

-- Step 2: Copy data from price to id_card (convert DECIMAL to VARCHAR)
-- This will convert existing price values to strings
UPDATE customers SET id_card = CAST(price AS VARCHAR) WHERE price IS NOT NULL;

-- Step 3: Drop the old price column
ALTER TABLE customers DROP COLUMN price;

-- Step 4: Add comment to the new column
COMMENT ON COLUMN customers.id_card IS 'Identity card number (allows digits and English letters only)';
