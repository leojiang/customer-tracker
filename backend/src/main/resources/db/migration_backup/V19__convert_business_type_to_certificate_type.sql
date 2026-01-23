-- V19: Convert business_type to certificate_type enum
-- Convert existing business_type text values to standardized certificate type enum values

-- First, convert existing business_type values to new certificate_type values
UPDATE customers
SET business_type = CASE
  WHEN LOWER(business_type) IN ('电工', 'electrician', 'dian gong') THEN 'ELECTRICIAN'
  WHEN LOWER(business_type) IN ('焊工', 'welder', 'han gong') THEN 'WELDER'
  WHEN LOWER(business_type) IN ('挖掘机', 'excavator', 'wa jue ji') THEN 'EXCAVATOR'
  ELSE NULL
END
WHERE business_type IS NOT NULL AND business_type != '';

-- Set any remaining null or empty values to NULL
UPDATE customers
SET business_type = NULL
WHERE business_type IS NULL OR business_type = '';

-- Add constraint to ensure only valid enum values
ALTER TABLE customers ADD CONSTRAINT check_certificate_type
CHECK (business_type IN ('ELECTRICIAN', 'WELDER', 'EXCAVATOR'));

-- Create index for certificate type queries
CREATE INDEX idx_customers_business_type ON customers(business_type) WHERE deleted_at IS NULL;