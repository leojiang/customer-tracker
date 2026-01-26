-- Add must_change_password column to sales table
-- This flag indicates whether a user must change their password on next login
-- Set to false by default for existing users
ALTER TABLE sales ADD COLUMN must_change_password BOOLEAN DEFAULT FALSE NOT NULL;
