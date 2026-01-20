-- Role System Update Migration
-- This migration updates the role system and adds customer delete request functionality

-- Step 1: Update existing SALES role to OFFICER
UPDATE sales SET role = 'OFFICER' WHERE role = 'SALES';

-- Step 2: Create customer_delete_requests table
CREATE TABLE IF NOT EXISTS customer_delete_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    requested_by UUID NOT NULL,
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by VARCHAR(20),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    rejection_reason VARCHAR(1000),
    CONSTRAINT fk_delete_request_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT fk_delete_request_requested_by FOREIGN KEY (requested_by) REFERENCES sales(id) ON DELETE CASCADE,
    CONSTRAINT chk_request_status CHECK (request_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- Step 3: Create indexes for performance
CREATE INDEX idx_delete_requests_status ON customer_delete_requests(request_status);
CREATE INDEX idx_delete_requests_customer ON customer_delete_requests(customer_id);
CREATE INDEX idx_delete_requests_requested_by ON customer_delete_requests(requested_by);
CREATE INDEX idx_delete_requests_created_at ON customer_delete_requests(created_at DESC);

-- Step 4: Add comments for documentation
COMMENT ON TABLE customer_delete_requests IS 'Stores customer deletion requests from officers awaiting admin approval';
COMMENT ON COLUMN customer_delete_requests.request_status IS 'Status of the delete request: PENDING, APPROVED, or REJECTED';
COMMENT ON COLUMN customer_delete_requests.reason IS 'Reason provided by officer for requesting customer deletion';
COMMENT ON COLUMN customer_delete_requests.rejection_reason IS 'Reason provided by admin for rejecting the request';
