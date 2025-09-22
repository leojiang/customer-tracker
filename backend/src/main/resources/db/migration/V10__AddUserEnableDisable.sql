-- Migration: Add user enable/disable functionality
-- This migration adds the ability for admins to enable/disable user accounts
-- while preserving the existing approval workflow

-- Add new columns for user enable/disable functionality (idempotent)
DO $$
BEGIN
    -- Add is_enabled column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'sales' AND column_name = 'is_enabled') THEN
        ALTER TABLE sales ADD COLUMN is_enabled BOOLEAN DEFAULT TRUE;
    END IF;
    
    -- Add disabled_at column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'sales' AND column_name = 'disabled_at') THEN
        ALTER TABLE sales ADD COLUMN disabled_at TIMESTAMP NULL;
    END IF;
    
    -- Add disabled_by_phone column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'sales' AND column_name = 'disabled_by_phone') THEN
        ALTER TABLE sales ADD COLUMN disabled_by_phone VARCHAR(20) NULL;
    END IF;
    
    -- Add disabled_reason column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'sales' AND column_name = 'disabled_reason') THEN
        ALTER TABLE sales ADD COLUMN disabled_reason TEXT NULL;
    END IF;
END $$;

-- Update existing approved users to be enabled
UPDATE sales SET is_enabled = TRUE WHERE approval_status = 'APPROVED' AND is_enabled IS NULL;

-- Update pending and rejected users to be disabled
UPDATE sales SET is_enabled = FALSE WHERE approval_status IN ('PENDING', 'REJECTED') AND is_enabled IS NULL;

-- Add indexes for performance (idempotent)
CREATE INDEX IF NOT EXISTS idx_sales_enabled ON sales(is_enabled);
CREATE INDEX IF NOT EXISTS idx_sales_disabled_at ON sales(disabled_at);

-- Add comments for documentation
COMMENT ON COLUMN sales.is_enabled IS 'Whether the user account is enabled for login';
COMMENT ON COLUMN sales.disabled_at IS 'Timestamp when the account was disabled';
COMMENT ON COLUMN sales.disabled_by_phone IS 'Phone number of admin who disabled the account';
COMMENT ON COLUMN sales.disabled_reason IS 'Reason for disabling the account';