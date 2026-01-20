-- Fix role column length in sales table
-- This migration increases the role column from VARCHAR(10) to VARCHAR(20)
-- to support the new role 'CUSTOMER_AGENT' which is 14 characters

-- Alter role column to VARCHAR(20)
ALTER TABLE sales ALTER COLUMN role TYPE VARCHAR(20);
