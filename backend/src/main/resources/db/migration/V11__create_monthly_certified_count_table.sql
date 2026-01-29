-- Create monthly_certified_count table
-- This table tracks the number of customers who reached CERTIFIED status each month

CREATE TABLE monthly_certified_count (
    month VARCHAR(7) PRIMARY KEY COMMENT 'Month in yyyy-mm format (e.g., 2024-01)',
    certified_count INTEGER NOT NULL DEFAULT 0 COMMENT 'Number of customers certified in this month',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT valid_month_format CHECK (month REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT non_negative_count CHECK (certified_count >= 0)
) COMMENT='Monthly count of customers who reached CERTIFIED status';

-- Index for efficient querying by month range
CREATE INDEX idx_monthly_certified_count_month ON monthly_certified_count(month);
