-- Add changed_fields column to customer_staging table
-- This field stores comma-separated list of field names that have changed
-- Only populated for records with import_status = 'UPDATE'
ALTER TABLE customer_staging ADD COLUMN changed_fields VARCHAR(255);
