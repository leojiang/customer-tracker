-- Rename price column to id_card (MySQL Version)
-- This migration changes the price DECIMAL column to id_card VARCHAR column
-- to store identity card numbers instead of price values

-- Step 1: Add the new id_card column
ALTER TABLE customers ADD COLUMN id_card VARCHAR(255) COMMENT 'Identity card number (allows digits and English letters only)';

-- Step 2: Copy data from price to id_card (convert DECIMAL to VARCHAR)
-- This will convert existing price values to strings
UPDATE customers SET id_card = CAST(price AS CHAR) WHERE price IS NOT NULL;

-- Step 3: Drop the old price column
ALTER TABLE customers DROP COLUMN price;
