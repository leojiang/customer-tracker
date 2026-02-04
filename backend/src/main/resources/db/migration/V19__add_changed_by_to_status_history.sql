-- Add changed_by column to status_history table
-- This column stores the name/identifier of the user who made the status transition

ALTER TABLE status_history
ADD COLUMN changed_by VARCHAR(255) COMMENT 'Name or identifier of the user who made this status transition';
