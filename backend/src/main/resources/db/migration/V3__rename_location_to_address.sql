-- Rename location column to address (MySQL Version)
-- This migration changes the location column to address for better clarity

-- Rename the column (MySQL uses CHANGE COLUMN syntax)
ALTER TABLE customers CHANGE COLUMN location address VARCHAR(500) COMMENT 'Customer address';
