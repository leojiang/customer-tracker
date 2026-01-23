-- Rename company column to certificate_issuer
ALTER TABLE customers RENAME COLUMN company TO certificate_issuer;

-- Update column comment
COMMENT ON COLUMN customers.certificate_issuer IS 'Certificate issuing organization/authority';
