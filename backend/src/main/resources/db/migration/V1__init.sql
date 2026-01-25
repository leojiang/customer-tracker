-- ============================================================================
-- CONSOLIDATED DATABASE SCHEMA INITIALIZATION (MySQL Version)
-- ============================================================================
-- This file represents the final schema state after migrations V1 through V25
-- It consolidates all tables, indexes, constraints, and initial data into one file
-- ============================================================================

-- ============================================================================
-- SECTION: SALES TABLE (User Management and Authentication)
-- ============================================================================
-- Contains user accounts with roles: ADMIN, OFFICER, CUSTOMER_AGENT
-- Includes approval workflow, enable/disable functionality, and proper constraints

CREATE TABLE sales (
    id BINARY(16) PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'OFFICER' COMMENT 'User role: ADMIN, OFFICER, CUSTOMER_AGENT',

    -- Approval system columns
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'Approval workflow status: PENDING, APPROVED, REJECTED',
    approved_by_phone VARCHAR(20),
    approved_at DATETIME,
    rejection_reason TEXT,
    status_updated_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when approval status was last updated',

    -- Enable/disable functionality
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '1 if enabled, 0 if disabled',
    disabled_at DATETIME COMMENT 'Timestamp when the account was disabled',
    disabled_by_phone VARCHAR(20),
    disabled_reason TEXT COMMENT 'Reason for disabling the account',

    -- Audit timestamps
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,

    -- Constraints
    CONSTRAINT unique_sales_phone UNIQUE (phone),
    CONSTRAINT valid_role CHECK (role IN ('ADMIN', 'OFFICER', 'CUSTOMER_AGENT')),
    CONSTRAINT check_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT fk_sales_approver FOREIGN KEY (approved_by_phone) REFERENCES sales(phone),
    CONSTRAINT fk_sales_disabled_by FOREIGN KEY (disabled_by_phone) REFERENCES sales(phone)
) COMMENT='User accounts for system authentication with role-based access control';

-- Indexes for sales table
CREATE INDEX idx_sales_phone ON sales(phone);
CREATE INDEX idx_sales_approval_status ON sales(approval_status, created_at);
CREATE INDEX idx_sales_approver ON sales(approved_by_phone);
CREATE INDEX idx_sales_status_updated ON sales(status_updated_at DESC);
CREATE INDEX idx_sales_enabled ON sales(is_enabled);
CREATE INDEX idx_sales_disabled_at ON sales(disabled_at);

-- ============================================================================
-- SECTION: CUSTOMERS TABLE
-- ============================================================================
-- Core customer data with certificate tracking, status management, and audit trail

CREATE TABLE customers (
    id BINARY(16) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    certificate_issuer VARCHAR(255) COMMENT 'Certificate issuing organization/authority',
    certificate_type VARCHAR(100) COMMENT 'Certificate type from CertificateType enum',
    customer_agent VARCHAR(255) COMMENT 'The agent who introduced this customer into the business',

    -- Business and demographic information
    business_requirements TEXT,
    age INTEGER,
    education VARCHAR(50) COMMENT 'Education level: ELEMENTARY, MIDDLE_SCHOOL, HIGH_SCHOOL, etc.',
    gender VARCHAR(50),
    location VARCHAR(500),

    -- Status and tracking
    current_status VARCHAR(50) NOT NULL DEFAULT 'NEW' COMMENT 'Customer status: NEW, NOTIFIED, ABORTED, SUBMITTED, CERTIFIED',
    price DECIMAL(19,2) COMMENT 'Customer price/amount in decimal format with 2 decimal places',
    certified_at DATETIME,

    -- Sales relationship
    sales_phone VARCHAR(20),

    -- Audit timestamps
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,

    -- Constraints
    CONSTRAINT unique_phone_certificate_type UNIQUE (phone, certificate_type),
    CONSTRAINT fk_customers_sales FOREIGN KEY (sales_phone) REFERENCES sales(phone),
    CONSTRAINT customers_current_status_check
        CHECK (current_status IN ('NEW', 'NOTIFIED', 'ABORTED', 'SUBMITTED', 'CERTIFIED')),
    CONSTRAINT check_education_level
        CHECK (education IN ('ELEMENTARY', 'MIDDLE_SCHOOL', 'HIGH_SCHOOL', 'ASSOCIATE', 'SECONDARY_VOCATIONAL',
                            'BACHELOR', 'MASTER', 'DOCTORATE', 'PROFESSIONAL', 'CERTIFICATE', 'OTHER'))
) COMMENT='Customer records with certificate tracking and status management';

-- Indexes for customers table
CREATE INDEX idx_customers_not_deleted_updated ON customers(updated_at DESC);
CREATE INDEX idx_customers_name ON customers(name);
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_sales_phone ON customers(sales_phone);
CREATE INDEX idx_customers_created_at ON customers(created_at DESC);
CREATE INDEX idx_customers_status ON customers(current_status);
CREATE INDEX idx_customers_status_sales ON customers(current_status, sales_phone);
CREATE INDEX idx_customers_education ON customers(education);
CREATE INDEX idx_customers_certificate_type ON customers(certificate_type);
CREATE INDEX idx_customers_phone_certificate ON customers(phone, certificate_type);

-- ============================================================================
-- SECTION: STATUS HISTORY TABLE
-- ============================================================================
-- Tracks all status changes for customers with audit trail

CREATE TABLE status_history (
    id BINARY(16) PRIMARY KEY,
    customer_id BINARY(16) NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    reason TEXT,
    sales_phone VARCHAR(20),
    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_status_history_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT status_history_to_status_check
        CHECK (to_status IN ('NEW', 'NOTIFIED', 'ABORTED', 'SUBMITTED', 'CERTIFIED')),
    CONSTRAINT status_history_from_status_check
        CHECK (from_status IN ('NEW', 'NOTIFIED', 'ABORTED', 'SUBMITTED', 'CERTIFIED') OR from_status IS NULL)
);

-- Indexes for status_history table
CREATE INDEX idx_status_history_customer_time ON status_history(customer_id, changed_at DESC);
CREATE INDEX idx_status_history_changed_at ON status_history(changed_at DESC);
CREATE INDEX idx_status_history_to_status ON status_history(to_status, changed_at DESC);
CREATE INDEX idx_status_history_sales_date ON status_history(sales_phone, changed_at DESC);

-- ============================================================================
-- SECTION: CUSTOMER DELETE REQUESTS TABLE
-- ============================================================================
-- Manages customer deletion requests from officers requiring admin approval

CREATE TABLE customer_delete_requests (
    id BINARY(16) PRIMARY KEY,
    customer_id BINARY(16) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    requested_by BINARY(16) NOT NULL,
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'Status of the delete request: PENDING, APPROVED, or REJECTED',
    reason VARCHAR(1000) NOT NULL COMMENT 'Reason provided by officer for requesting customer deletion',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by VARCHAR(20),
    reviewed_at DATETIME,
    rejection_reason VARCHAR(1000) COMMENT 'Reason provided by admin for rejecting the request',

    -- Constraints
    CONSTRAINT fk_delete_request_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT fk_delete_request_requested_by FOREIGN KEY (requested_by) REFERENCES sales(id) ON DELETE CASCADE,
    CONSTRAINT chk_request_status CHECK (request_status IN ('PENDING', 'APPROVED', 'REJECTED'))
) COMMENT='Stores customer deletion requests from officers awaiting admin approval';

-- Indexes for customer_delete_requests table
CREATE INDEX idx_delete_requests_status ON customer_delete_requests(request_status);
CREATE INDEX idx_delete_requests_customer ON customer_delete_requests(customer_id);
CREATE INDEX idx_delete_requests_requested_by ON customer_delete_requests(requested_by);
CREATE INDEX idx_delete_requests_created_at ON customer_delete_requests(created_at DESC);

-- ============================================================================
-- SECTION: USER APPROVAL HISTORY TABLE
-- ============================================================================
-- Audit trail for all user approval and enable/disable actions

CREATE TABLE user_approval_history (
    id BINARY(16) PRIMARY KEY,
    user_phone VARCHAR(20) NOT NULL,
    action VARCHAR(20) NOT NULL COMMENT 'Action type: APPROVED, REJECTED, RESET, PENDING, ENABLED, DISABLED',
    admin_phone VARCHAR(20) NOT NULL,
    reason TEXT,
    action_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_approval_history_user
        FOREIGN KEY (user_phone) REFERENCES sales(phone) ON DELETE CASCADE,
    CONSTRAINT fk_approval_history_admin
        FOREIGN KEY (admin_phone) REFERENCES sales(phone),
    CONSTRAINT check_approval_action
        CHECK (action IN ('APPROVED', 'REJECTED', 'RESET', 'PENDING', 'ENABLED', 'DISABLED'))
);

-- Indexes for user_approval_history table
CREATE INDEX idx_user_approval_history_user
    ON user_approval_history(user_phone, action_timestamp DESC);
CREATE INDEX idx_user_approval_history_admin
    ON user_approval_history(admin_phone, action_timestamp DESC);
CREATE INDEX idx_user_approval_history_action
    ON user_approval_history(action, action_timestamp DESC);

-- ============================================================================
-- SECTION: ANALYTICS SNAPSHOTS TABLE
-- ============================================================================
-- Pre-aggregated analytics data for dashboard performance optimization

CREATE TABLE analytics_snapshots (
    id BINARY(16) PRIMARY KEY,
    snapshot_date DATE NOT NULL,
    sales_phone VARCHAR(20) COMMENT 'NULL for system-wide metrics, specific phone for sales-specific metrics',
    metric_type VARCHAR(50) NOT NULL COMMENT 'Aggregation period: daily, weekly, monthly',

    -- Customer metrics
    total_customers INTEGER DEFAULT 0,
    new_customers INTEGER DEFAULT 0,
    active_customers INTEGER DEFAULT 0,
    converted_customers INTEGER DEFAULT 0,
    lost_customers INTEGER DEFAULT 0,

    -- Status distribution
    customers_called INTEGER DEFAULT 0,
    customers_replied INTEGER DEFAULT 0,
    orders_placed INTEGER DEFAULT 0,
    orders_cancelled INTEGER DEFAULT 0,
    products_delivered INTEGER DEFAULT 0,
    business_done INTEGER DEFAULT 0,

    -- Performance metrics
    conversion_rate DECIMAL(5,2),
    avg_cycle_time_days DECIMAL(8,2),

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    UNIQUE(snapshot_date, sales_phone, metric_type)
) COMMENT='Pre-aggregated analytics data for dashboard performance optimization';

-- Indexes for analytics_snapshots table
CREATE INDEX idx_analytics_snapshots_date_sales ON analytics_snapshots(snapshot_date, sales_phone);
CREATE INDEX idx_analytics_snapshots_type ON analytics_snapshots(metric_type);

-- ============================================================================
-- SECTION: INITIAL DATA
-- ============================================================================

-- Insert hardcoded admin user with phone 18980994001 and password 123456
-- Password is BCrypt hash of '123456' with strength 10
INSERT INTO sales (id, phone, password, role, approval_status, approved_at, status_updated_at, is_enabled)
VALUES (
    UUID_TO_BIN(UUID()),
    '18980994001',
    '$2a$10$4KzCUhnd9TmmlpjKAvlsSegh0jvLsq5BEaognpyp/6thn1nbTUOAO',
    'ADMIN',
    'APPROVED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    1
);

-- Create initial approval history for admin user
INSERT INTO user_approval_history (id, user_phone, action, admin_phone, reason, action_timestamp)
VALUES (
    UUID_TO_BIN(UUID()),
    '18980994001',
    'APPROVED',
    '18980994001',
    'System initialization - admin account',
    CURRENT_TIMESTAMP
);

-- ============================================================================
-- END OF CONSOLIDATED SCHEMA (MySQL Version)
-- ============================================================================
