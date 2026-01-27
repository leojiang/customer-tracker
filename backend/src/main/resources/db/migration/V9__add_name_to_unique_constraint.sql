-- Add name to unique constraint for customers table
-- This changes the unique identifier from (phone, certificate_type) to (name, phone, certificate_type)

-- Drop the old unique constraint
ALTER TABLE customers DROP INDEX unique_phone_certificate_type;

-- Add the new unique constraint with name included
ALTER TABLE customers ADD CONSTRAINT unique_name_phone_certificate_type UNIQUE (name, phone, certificate_type);
