-- Fix phone column length in sales table
-- This migration increases the phone column length from VARCHAR(10) to VARCHAR(20)
-- to support phone numbers with country codes and formatting

-- Step 1: Alter phone column in sales table
ALTER TABLE sales ALTER COLUMN phone TYPE VARCHAR(20);

-- Step 2: Alter approved_by_phone column (also needs to be VARCHAR(20))
ALTER TABLE sales ALTER COLUMN approved_by_phone TYPE VARCHAR(20);

-- Step 3: Alter disabled_by_phone column
ALTER TABLE sales ALTER COLUMN disabled_by_phone TYPE VARCHAR(20);
