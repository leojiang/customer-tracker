-- Create monthly_agent_performance table
-- This table stores monthly aggregated performance metrics for each customer agent
-- Data is immutable once created (historical snapshots)

CREATE TABLE monthly_agent_performance (
    customer_agent VARCHAR(100) NOT NULL COMMENT 'Customer agent identifier (phone or name)',
    month VARCHAR(7) NOT NULL COMMENT 'Month in YYYY-MM format',
    total_customers INT NOT NULL DEFAULT 0 COMMENT 'Total customers assigned to agent',
    new_customers INT NOT NULL DEFAULT 0 COMMENT 'New customers acquired in this month',
    conversions INT NOT NULL DEFAULT 0 COMMENT 'Customers certified in this month',
    conversion_rate DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT 'Conversion rate (conversions/total_customers * 100)',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record update timestamp',
    PRIMARY KEY (customer_agent, month),
    INDEX idx_month (month),
    INDEX idx_customer_agent (customer_agent)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Monthly performance metrics by customer agent';
