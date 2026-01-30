-- Change unique constraint from (name, phone, certificate_type) to (id_card, certificate_type)
-- This migration:
-- 1. Generates placeholder id_card values for customers without one
-- 2. Makes id_card NOT NULL
-- 3. Makes phone nullable (phone is optional now)
-- 4. Drops old unique constraint (name, phone, certificate_type)
-- 5. Adds new unique constraint (id_card, certificate_type)

-- Step 1: Generate placeholder id_card for customers who don't have one
-- Use pattern: PLACEHOLDER_<UUID> to ensure uniqueness
UPDATE customers
SET id_card = CONCAT('PLACEHOLDER_', UUID())
WHERE deleted_at IS NULL
  AND (id_card IS NULL OR id_card = '');

-- Step 2: Make phone column nullable (phone is now optional)
ALTER TABLE customers MODIFY phone VARCHAR(20) NULL;

-- Step 3: Make id_card NOT NULL (all records now have values)
ALTER TABLE customers MODIFY id_card VARCHAR(64) NOT NULL;

-- Step 4: Drop old unique constraint (name, phone, certificate_type)
ALTER TABLE customers DROP INDEX unique_name_phone_certificate_type;

-- Step 5: Add new unique constraint (id_card, certificate_type)
ALTER TABLE customers
ADD CONSTRAINT unique_id_card_certificate_type
UNIQUE (id_card, certificate_type);
