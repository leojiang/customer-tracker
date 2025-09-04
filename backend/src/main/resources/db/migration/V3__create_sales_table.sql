-- Create sales table for authentication
CREATE TABLE sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'SALES',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT unique_sales_phone UNIQUE (phone),
    CONSTRAINT valid_role CHECK (role IN ('ADMIN', 'SALES'))
);

-- Create index for phone lookups
CREATE INDEX idx_sales_phone ON sales(phone) WHERE deleted_at IS NULL;

-- Insert hardcoded admin user with phone 18980994001 and password 123456
-- Password is BCrypt hash of '123456' with strength 10
INSERT INTO sales (phone, password, role) VALUES 
('18980994001', '$2a$10$N9qo8uLOickgx2ZMRZoMye7c8.P/LWP5B8XNJfvfF.X/PXCm0p.42', 'ADMIN');