-- Backfill monthly_agent_performance table
-- This script aggregates historical data from the customers table
-- Run this script after creating the table to populate it with historical data

-- Clear any existing data
TRUNCATE TABLE monthly_agent_performance;

-- Insert aggregated data by customer agent and month
-- Group customers by certification date to show monthly performance trends
-- Uses window function to calculate cumulative totals
INSERT INTO monthly_agent_performance (
    customer_agent,
    month,
    total_customers,
    new_customers,
    conversions,
    conversion_rate
)
WITH monthly_certifications AS (
    SELECT
        customer_agent,
        DATE_FORMAT(certified_at, '%Y-%m') AS month,
        COUNT(*) AS customers_certified_this_month
    FROM customers
    WHERE customer_agent IS NOT NULL
      AND certified_at IS NOT NULL
      AND deleted_at IS NULL
    GROUP BY customer_agent, DATE_FORMAT(certified_at, '%Y-%m')
),
cumulative_totals AS (
    SELECT
        customer_agent,
        month,
        customers_certified_this_month AS new_customers,
        customers_certified_this_month AS conversions,
        SUM(customers_certified_this_month) OVER (
            PARTITION BY customer_agent
            ORDER BY month
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS total_customers
    FROM monthly_certifications
)
SELECT
    customer_agent,
    month,
    total_customers,
    new_customers,
    conversions,
    ROUND(
        CASE
            WHEN total_customers > 0 THEN
                (conversions * 100.0 / total_customers)
            ELSE 0
        END,
        2
    ) AS conversion_rate
FROM cumulative_totals
ORDER BY month DESC, customer_agent;

-- Verify the backfill
-- SELECT * FROM monthly_agent_performance ORDER BY month DESC, customer_agent;
-- SELECT COUNT(*) as total_records FROM monthly_agent_performance;
-- SELECT SUM(conversions) as total_conversions FROM monthly_agent_performance;
