-- V12: Fix user approval history constraint to include ENABLED and DISABLED actions
-- The original constraint only allowed APPROVED, REJECTED, RESET, PENDING
-- but the Java code also uses ENABLED and DISABLED actions

-- Drop the existing constraint
ALTER TABLE user_approval_history DROP CONSTRAINT check_approval_action;

-- Add the updated constraint with all supported actions
ALTER TABLE user_approval_history ADD CONSTRAINT check_approval_action 
    CHECK (action IN ('APPROVED', 'REJECTED', 'RESET', 'PENDING', 'ENABLED', 'DISABLED'));