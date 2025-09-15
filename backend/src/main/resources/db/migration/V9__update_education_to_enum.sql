-- V9: Update education field to use enum values
-- Convert existing education text values to standardized enum values

-- First, create a mapping of common education values to enum values
UPDATE customers 
SET education = CASE 
  WHEN LOWER(education) IN ('elementary', 'elementary school', 'primary', 'primary school') THEN 'ELEMENTARY'
  WHEN LOWER(education) IN ('middle school', 'junior high', 'intermediate') THEN 'MIDDLE_SCHOOL'
  WHEN LOWER(education) IN ('high school', 'secondary', 'secondary school', 'hs') THEN 'HIGH_SCHOOL'
  WHEN LOWER(education) IN ('associate', 'associate degree', 'aa', 'as') THEN 'ASSOCIATE'
  WHEN LOWER(education) IN ('bachelor', 'bachelor''s', 'bachelor''s degree', 'ba', 'bs', 'bachelor degree') THEN 'BACHELOR'
  WHEN LOWER(education) IN ('master', 'master''s', 'master''s degree', 'ma', 'ms', 'master degree') THEN 'MASTER'
  WHEN LOWER(education) IN ('doctorate', 'phd', 'ph.d.', 'doctor', 'doctoral') THEN 'DOCTORATE'
  WHEN LOWER(education) IN ('professional', 'professional degree', 'jd', 'md', 'dds') THEN 'PROFESSIONAL'
  WHEN LOWER(education) IN ('certificate', 'diploma', 'cert', 'certification') THEN 'CERTIFICATE'
  ELSE 'OTHER'
END
WHERE education IS NOT NULL AND education != '';

-- Set any remaining null or empty values to NULL (optional)
UPDATE customers 
SET education = NULL 
WHERE education IS NULL OR education = '';

-- Add constraint to ensure only valid enum values
ALTER TABLE customers ADD CONSTRAINT check_education_level 
CHECK (education IN ('ELEMENTARY', 'MIDDLE_SCHOOL', 'HIGH_SCHOOL', 'ASSOCIATE', 'BACHELOR', 'MASTER', 'DOCTORATE', 'PROFESSIONAL', 'CERTIFICATE', 'OTHER'));

-- Create index for education queries
CREATE INDEX idx_customers_education ON customers(education) WHERE deleted_at IS NULL;