-- V7: Add User Approval System
-- Add approval system columns to sales table and create approval history table

-- Add approval system columns to sales table
ALTER TABLE sales ADD COLUMN approval_status VARCHAR(20) DEFAULT 'APPROVED';
ALTER TABLE sales ADD COLUMN approved_by_phone VARCHAR(20);
ALTER TABLE sales ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE sales ADD COLUMN rejection_reason TEXT;
ALTER TABLE sales ADD COLUMN status_updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Add constraint for valid approval statuses
ALTER TABLE sales ADD CONSTRAINT check_approval_status 
CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- Add foreign key for approver (self-reference)
ALTER TABLE sales ADD CONSTRAINT fk_sales_approver 
FOREIGN KEY (approved_by_phone) REFERENCES sales(phone);

-- Create indexes for performance
CREATE INDEX idx_sales_approval_status ON sales(approval_status, created_at);
CREATE INDEX idx_sales_approver ON sales(approved_by_phone);
CREATE INDEX idx_sales_status_updated ON sales(status_updated_at DESC);

-- Create user approval history table
CREATE TABLE user_approval_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_phone VARCHAR(20) NOT NULL,
    action VARCHAR(20) NOT NULL,
    admin_phone VARCHAR(20) NOT NULL,
    reason TEXT,
    action_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_approval_history_user 
        FOREIGN KEY (user_phone) REFERENCES sales(phone) ON DELETE CASCADE,
    CONSTRAINT fk_approval_history_admin 
        FOREIGN KEY (admin_phone) REFERENCES sales(phone),
    CONSTRAINT check_approval_action 
        CHECK (action IN ('APPROVED', 'REJECTED', 'RESET', 'PENDING'))
);

-- Create indexes for audit queries
CREATE INDEX idx_user_approval_history_user 
    ON user_approval_history(user_phone, action_timestamp DESC);
CREATE INDEX idx_user_approval_history_admin 
    ON user_approval_history(admin_phone, action_timestamp DESC);
CREATE INDEX idx_user_approval_history_action 
    ON user_approval_history(action, action_timestamp DESC);

-- Update existing admin users to APPROVED status and set approved_at
UPDATE sales 
SET approval_status = 'APPROVED', 
    approved_at = created_at,
    status_updated_at = created_at
WHERE role = 'ADMIN';

-- Set new registrations to PENDING by default (update default value)
ALTER TABLE sales ALTER COLUMN approval_status SET DEFAULT 'PENDING';

-- Create initial approval history for existing users
INSERT INTO user_approval_history (user_phone, action, admin_phone, reason, action_timestamp)
SELECT s.phone, 'APPROVED', s.phone, 'System migration - existing user', s.created_at
FROM sales s
WHERE s.role = 'ADMIN';