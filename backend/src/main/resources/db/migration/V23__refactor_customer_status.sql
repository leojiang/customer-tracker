-- Refactor customer status to new simplified status system
-- New statuses: NEW, NOTIFIED, ABORTED, SUBMITTED, CERTIFIED

-- Step 1: Drop existing constraints before data migration
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_current_status_check;
ALTER TABLE status_history DROP CONSTRAINT IF EXISTS status_history_to_status_check;
ALTER TABLE status_history DROP CONSTRAINT IF EXISTS status_history_from_status_check;

-- Step 2: Update existing customer records to new statuses
UPDATE customers
SET current_status = 'NEW'
WHERE current_status = 'CUSTOMER_CALLED';

UPDATE customers
SET current_status = 'NOTIFIED'
WHERE current_status = 'REPLIED_TO_CUSTOMER';

UPDATE customers
SET current_status = 'SUBMITTED'
WHERE current_status IN ('ORDER_PLACED', 'PRODUCT_DELIVERED');

UPDATE customers
SET current_status = 'CERTIFIED'
WHERE current_status = 'BUSINESS_DONE';

UPDATE customers
SET current_status = 'ABORTED'
WHERE current_status IN ('LOST', 'ORDER_CANCELLED');

-- Step 3: Update status_history records - comprehensive update for all combinations
-- First update all to_status values
UPDATE status_history SET to_status = 'NEW' WHERE to_status = 'CUSTOMER_CALLED';
UPDATE status_history SET to_status = 'NOTIFIED' WHERE to_status = 'REPLIED_TO_CUSTOMER';
UPDATE status_history SET to_status = 'SUBMITTED' WHERE to_status IN ('ORDER_PLACED', 'PRODUCT_DELIVERED');
UPDATE status_history SET to_status = 'CERTIFIED' WHERE to_status = 'BUSINESS_DONE';
UPDATE status_history SET to_status = 'ABORTED' WHERE to_status IN ('LOST', 'ORDER_CANCELLED');

-- Then update all from_status values
UPDATE status_history SET from_status = 'NEW' WHERE from_status = 'CUSTOMER_CALLED';
UPDATE status_history SET from_status = 'NOTIFIED' WHERE from_status = 'REPLIED_TO_CUSTOMER';
UPDATE status_history SET from_status = 'SUBMITTED' WHERE from_status IN ('ORDER_PLACED', 'PRODUCT_DELIVERED');
UPDATE status_history SET from_status = 'CERTIFIED' WHERE from_status = 'BUSINESS_DONE';
UPDATE status_history SET from_status = 'ABORTED' WHERE from_status IN ('LOST', 'ORDER_CANCELLED');

-- Step 4: Add new check constraints with updated statuses
ALTER TABLE customers
ADD CONSTRAINT customers_current_status_check
CHECK (current_status IN ('NEW', 'NOTIFIED', 'ABORTED', 'SUBMITTED', 'CERTIFIED'));

ALTER TABLE status_history
ADD CONSTRAINT status_history_to_status_check
CHECK (to_status IN ('NEW', 'NOTIFIED', 'ABORTED', 'SUBMITTED', 'CERTIFIED'));

ALTER TABLE status_history
ADD CONSTRAINT status_history_from_status_check
CHECK (from_status IN ('NEW', 'NOTIFIED', 'ABORTED', 'SUBMITTED', 'CERTIFIED') OR from_status IS NULL);
