-- Add new_customer_certified_count and renew_customer_certified_count columns
-- These will track certifications broken down by customer type

-- Add the new columns
ALTER TABLE monthly_certified_count
ADD COLUMN new_customer_certified_count INTEGER NOT NULL DEFAULT 0 COMMENT 'Number of NEW_CUSTOMER type certifications' AFTER certified_count,
ADD COLUMN renew_customer_certified_count INTEGER NOT NULL DEFAULT 0 COMMENT 'Number of RENEW_CUSTOMER type certifications' AFTER new_customer_certified_count;

-- Add constraints for non-negative values
ALTER TABLE monthly_certified_count
ADD CONSTRAINT non_negative_new_customer_count CHECK (new_customer_certified_count >= 0),
ADD CONSTRAINT non_negative_renew_customer_count CHECK (renew_customer_certified_count >= 0);
