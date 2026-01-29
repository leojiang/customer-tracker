-- ============================================================================
-- Migration V10: Add CERTIFIED_ELSEWHERE status to customer status constraints
-- ============================================================================
-- This migration adds the new CERTIFIED_ELSEWHERE status to all CHECK constraints
-- and comments that reference customer status values.
--
-- Status Transition Rules:
-- - Any status can transition TO CERTIFIED_ELSEWHERE
-- - CERTIFIED_ELSEWHERE can transition to any non-NEW status
-- - CERTIFIED_ELSEWHERE CANNOT transition back to NEW
-- ============================================================================

-- Update the comment on customers.current_status to include CERTIFIED_ELSEWHERE
ALTER TABLE customers MODIFY COLUMN current_status VARCHAR(50) NOT NULL DEFAULT 'NEW'
    COMMENT 'Customer status: NEW, NOTIFIED, ABORTED, SUBMITTED, CERTIFIED, CERTIFIED_ELSEWHERE';

-- Drop the old CHECK constraint on customers.current_status
ALTER TABLE customers DROP CONSTRAINT customers_current_status_check;

-- Add the new CHECK constraint on customers.current_status with CERTIFIED_ELSEWHERE
ALTER TABLE customers ADD CONSTRAINT customers_current_status_check
    CHECK (current_status IN ('NEW', 'NOTIFIED', 'ABORTED', 'SUBMITTED', 'CERTIFIED', 'CERTIFIED_ELSEWHERE'));

-- Drop the old CHECK constraint on status_history.to_status
ALTER TABLE status_history DROP CONSTRAINT status_history_to_status_check;

-- Add the new CHECK constraint on status_history.to_status with CERTIFIED_ELSEWHERE
ALTER TABLE status_history ADD CONSTRAINT status_history_to_status_check
    CHECK (to_status IN ('NEW', 'NOTIFIED', 'ABORTED', 'SUBMITTED', 'CERTIFIED', 'CERTIFIED_ELSEWHERE'));

-- Drop the old CHECK constraint on status_history.from_status
ALTER TABLE status_history DROP CONSTRAINT status_history_from_status_check;

-- Add the new CHECK constraint on status_history.from_status with CERTIFIED_ELSEWHERE
ALTER TABLE status_history ADD CONSTRAINT status_history_from_status_check
    CHECK (from_status IN ('NEW', 'NOTIFIED', 'ABORTED', 'SUBMITTED', 'CERTIFIED', 'CERTIFIED_ELSEWHERE') OR from_status IS NULL);
