-- Migration: Add user enable/disable functionality
-- This migration adds the ability for admins to enable/disable user accounts
-- while preserving the existing approval workflow

-- Add new columns for user enable/disable functionality
ALTER TABLE sales ADD COLUMN is_enabled BOOLEAN DEFAULT TRUE;
ALTER TABLE sales ADD COLUMN disabled_at TIMESTAMP NULL;
ALTER TABLE sales ADD COLUMN disabled_by_phone VARCHAR(20) NULL;
ALTER TABLE sales ADD COLUMN disabled_reason TEXT NULL;

-- Update existing approved users to be enabled
UPDATE sales SET is_enabled = TRUE WHERE approval_status = 'APPROVED';

-- Update pending and rejected users to be disabled
UPDATE sales SET is_enabled = FALSE WHERE approval_status IN ('PENDING', 'REJECTED');

-- Add indexes for performance
CREATE INDEX idx_sales_enabled ON sales(is_enabled);
CREATE INDEX idx_sales_disabled_at ON sales(disabled_at);

-- Add comments for documentation
COMMENT ON COLUMN sales.is_enabled IS 'Whether the user account is enabled for login';
COMMENT ON COLUMN sales.disabled_at IS 'Timestamp when the account was disabled';
COMMENT ON COLUMN sales.disabled_by_phone IS 'Phone number of admin who disabled the account';
COMMENT ON COLUMN sales.disabled_reason IS 'Reason for disabling the account';