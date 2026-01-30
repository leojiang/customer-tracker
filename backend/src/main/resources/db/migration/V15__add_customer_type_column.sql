-- Add customer_type column to customers and customer_staging tables
-- This field distinguishes between new customers and renewing customers

-- Add customer_type column to customers table
ALTER TABLE customers
ADD COLUMN customer_type VARCHAR(50) NOT NULL DEFAULT 'NEW_CUSTOMER'
COMMENT 'Customer type: NEW_CUSTOMER or RENEW_CUSTOMER';

-- Add customer_type column to customer_staging table
ALTER TABLE customer_staging
ADD COLUMN customer_type VARCHAR(50) NULL
COMMENT 'Customer type: NEW_CUSTOMER or RENEW_CUSTOMER';
