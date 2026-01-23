-- ============================================================================
-- CONSOLIDATED DATABASE SCHEMA INITIALIZATION
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
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'OFFICER',

    -- Approval system columns
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by_phone VARCHAR(20),
    approved_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    status_updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Enable/disable functionality
    is_enabled BOOLEAN DEFAULT TRUE,
    disabled_at TIMESTAMP WITH TIME ZONE,
    disabled_by_phone VARCHAR(20),
    disabled_reason TEXT,

    -- Audit timestamps
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,

    -- Constraints
    CONSTRAINT unique_sales_phone UNIQUE (phone),
    CONSTRAINT valid_role CHECK (role IN ('ADMIN', 'OFFICER', 'CUSTOMER_AGENT')),
    CONSTRAINT check_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT fk_sales_approver FOREIGN KEY (approved_by_phone) REFERENCES sales(phone),
    CONSTRAINT fk_sales_disabled_by FOREIGN KEY (disabled_by_phone) REFERENCES sales(phone)
);

-- Indexes for sales table
CREATE INDEX idx_sales_phone ON sales(phone) WHERE deleted_at IS NULL;
CREATE INDEX idx_sales_approval_status ON sales(approval_status, created_at);
CREATE INDEX idx_sales_approver ON sales(approved_by_phone);
CREATE INDEX idx_sales_status_updated ON sales(status_updated_at DESC);
CREATE INDEX idx_sales_enabled ON sales(is_enabled);
CREATE INDEX idx_sales_disabled_at ON sales(disabled_at);

-- Comments for sales table
COMMENT ON TABLE sales IS 'User accounts for system authentication with role-based access control';
COMMENT ON COLUMN sales.approval_status IS 'Approval workflow status: PENDING, APPROVED, REJECTED';
COMMENT ON COLUMN sales.is_enabled IS 'Whether the user account is enabled for login';
COMMENT ON COLUMN sales.disabled_at IS 'Timestamp when the account was disabled';
COMMENT ON COLUMN sales.disabled_by_phone IS 'Phone number of admin who disabled the account';
COMMENT ON COLUMN sales.disabled_reason IS 'Reason for disabling the account';

-- ============================================================================
-- SECTION: CUSTOMERS TABLE
-- ============================================================================
-- Core customer data with certificate tracking, status management, and audit trail

CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    certificate_issuer TEXT,
    certificate_type TEXT,
    customer_agent VARCHAR(255),

    -- Business and demographic information
    business_requirements TEXT,
    age INTEGER,
    education TEXT,
    gender TEXT,
    location TEXT,

    -- Status and tracking
    current_status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    price DECIMAL(19,2),
    certified_at TIMESTAMP,

    -- Sales relationship
    sales_phone VARCHAR(20),

    -- Audit timestamps
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT unique_phone_certificate_type UNIQUE (phone, certificate_type),
    CONSTRAINT fk_customers_sales FOREIGN KEY (sales_phone) REFERENCES sales(phone),
    CONSTRAINT customers_current_status_check
        CHECK (current_status IN ('NEW', 'NOTIFIED', 'ABORTED', 'SUBMITTED', 'CERTIFIED')),
    CONSTRAINT check_education_level
        CHECK (education IN ('ELEMENTARY', 'MIDDLE_SCHOOL', 'HIGH_SCHOOL', 'ASSOCIATE',
                            'BACHELOR', 'MASTER', 'DOCTORATE', 'PROFESSIONAL', 'CERTIFICATE', 'OTHER'))
);

-- Indexes for customers table
CREATE INDEX idx_customers_not_deleted_updated ON customers(updated_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_name ON customers(LOWER(name)) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_sales_phone ON customers(sales_phone) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_created_at ON customers(created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_status ON customers(current_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_status_sales ON customers(current_status, sales_phone) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_education ON customers(education) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_certificate_type ON customers(certificate_type) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_phone_certificate ON customers(phone, certificate_type) WHERE deleted_at IS NULL;

-- Comments for customers table
COMMENT ON TABLE customers IS 'Customer records with certificate tracking and status management';
COMMENT ON COLUMN customers.certificate_type IS 'Certificate type from CertificateType enum: Q1_COMMAND, Q2_MOBILE_CRANE, Q2_BRIDGE_CRANE, Q2_GANTRY_CRANE, Q2_TOWER_CRANE, Q2_HOIST, N1_FORKLIFT, N2_SIGHTSEEING_CAR, G1_INDUSTRIAL_BOILER, G3_BOILER_WATER_TREATMENT, R1_QUICK_OPEN_PRESSURE_VESSEL, R2_MOBILE_PRESSURE_VESSEL, P_GAS_FILLING, A_SPECIAL_EQUIPMENT_SAFETY, T_ELEVATOR_OPERATION, CONSTRUCTION_ELECTRICIAN, CONSTRUCTION_WELDER, CONSTRUCTION_SCAFFOLDER, CONSTRUCTION_LIFTING_EQUIPMENT, CONSTRUCTION_SIGNALMAN, CONSTRUCTION_MATERIAL_HOIST_DRIVER, CONSTRUCTION_GONDOLA_INSTALLER, LOW_VOLTAGE_ELECTRICIAN, WELDING_THERMAL_CUTTING, HIGH_VOLTAGE_ELECTRICIAN, HIGH_ALTITUDE_INSTALLATION, HIGH_ALTITUDE_SCAFFOLDING, REFRIGERATION_AIR_CONDITIONING, COAL_MINE_SAFETY, METAL_NONMETAL_MINE_SAFETY, OIL_GAS_SAFETY, HAZARDOUS_CHEMICALS_SAFETY, METALLURGY_SAFETY, FIREWORKS_SAFETY, OTHERS';
COMMENT ON COLUMN customers.certificate_issuer IS 'Certificate issuing organization/authority';
COMMENT ON COLUMN customers.customer_agent IS 'The agent who introduced this customer into the business';
COMMENT ON COLUMN customers.price IS 'Customer price/amount in decimal format with 2 decimal places';

-- ============================================================================
-- SECTION: STATUS HISTORY TABLE
-- ============================================================================
-- Tracks all status changes for customers with audit trail

CREATE TABLE status_history (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    reason TEXT,
    sales_phone VARCHAR(20),
    changed_at TIMESTAMPTZ DEFAULT NOW(),

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
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    requested_by UUID NOT NULL,
    request_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by VARCHAR(20),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    rejection_reason VARCHAR(1000),

    -- Constraints
    CONSTRAINT fk_delete_request_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
    CONSTRAINT fk_delete_request_requested_by FOREIGN KEY (requested_by) REFERENCES sales(id) ON DELETE CASCADE,
    CONSTRAINT chk_request_status CHECK (request_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- Indexes for customer_delete_requests table
CREATE INDEX idx_delete_requests_status ON customer_delete_requests(request_status);
CREATE INDEX idx_delete_requests_customer ON customer_delete_requests(customer_id);
CREATE INDEX idx_delete_requests_requested_by ON customer_delete_requests(requested_by);
CREATE INDEX idx_delete_requests_created_at ON customer_delete_requests(created_at DESC);

-- Comments for customer_delete_requests table
COMMENT ON TABLE customer_delete_requests IS 'Stores customer deletion requests from officers awaiting admin approval';
COMMENT ON COLUMN customer_delete_requests.request_status IS 'Status of the delete request: PENDING, APPROVED, or REJECTED';
COMMENT ON COLUMN customer_delete_requests.reason IS 'Reason provided by officer for requesting customer deletion';
COMMENT ON COLUMN customer_delete_requests.rejection_reason IS 'Reason provided by admin for rejecting the request';

-- ============================================================================
-- SECTION: USER APPROVAL HISTORY TABLE
-- ============================================================================
-- Audit trail for all user approval and enable/disable actions

CREATE TABLE user_approval_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_phone VARCHAR(20) NOT NULL,
    action VARCHAR(20) NOT NULL,
    admin_phone VARCHAR(20) NOT NULL,
    reason TEXT,
    action_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

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
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    snapshot_date DATE NOT NULL,
    sales_phone VARCHAR(20),
    metric_type VARCHAR(50) NOT NULL,

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

    created_at TIMESTAMPTZ DEFAULT NOW(),

    -- Constraints
    UNIQUE(snapshot_date, sales_phone, metric_type)
);

-- Indexes for analytics_snapshots table
CREATE INDEX idx_analytics_snapshots_date_sales ON analytics_snapshots(snapshot_date, sales_phone);
CREATE INDEX idx_analytics_snapshots_type ON analytics_snapshots(metric_type);

-- Comments for analytics_snapshots table
COMMENT ON TABLE analytics_snapshots IS 'Pre-aggregated analytics data for dashboard performance optimization';
COMMENT ON COLUMN analytics_snapshots.sales_phone IS 'NULL for system-wide metrics, specific phone for sales-specific metrics';
COMMENT ON COLUMN analytics_snapshots.metric_type IS 'Aggregation period: daily, weekly, monthly';

-- ============================================================================
-- SECTION: INITIAL DATA
-- ============================================================================

-- Insert hardcoded admin user with phone 18980994001 and password 123456
-- Password is BCrypt hash of '123456' with strength 10
INSERT INTO sales (phone, password, role, approval_status, approved_at, status_updated_at, is_enabled)
VALUES (
    '18980994001',
    '$2a$10$4KzCUhnd9TmmlpjKAvlsSegh0jvLsq5BEaognpyp/6thn1nbTUOAO',
    'ADMIN',
    'APPROVED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    TRUE
);

-- Create initial approval history for admin user
INSERT INTO user_approval_history (user_phone, action, admin_phone, reason, action_timestamp)
VALUES (
    '18980994001',
    'APPROVED',
    '18980994001',
    'System initialization - admin account',
    CURRENT_TIMESTAMP
);

-- ============================================================================
-- END OF CONSOLIDATED SCHEMA
-- ============================================================================
