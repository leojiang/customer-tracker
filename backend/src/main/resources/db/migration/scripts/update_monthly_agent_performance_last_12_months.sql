-- ============================================================================
-- UPDATE MONTHLY AGENT PERFORMANCE - LAST 12 MONTHS ONLY
-- ============================================================================
-- Purpose: Update monthly_agent_performance table with data from the last 12 months
-- Usage: Run this script after importing new customer data to refresh analytics
-- IMPORTANT: This script only affects the last 12 months, protecting historical data
-- ============================================================================

-- Step 1: Delete records from the last 12 months only
-- This preserves historical data older than 12 months
DELETE FROM monthly_agent_performance
WHERE month >= DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m');

-- Step 2: Insert/Update data for the last 12 months
-- Uses REPLACE INTO to handle the composite primary key (customer_agent, month)
-- Gets baseline cumulative total from most recent historical month before the update window
INSERT INTO monthly_agent_performance (
    customer_agent,
    month,
    total_customers,
    new_customers,
    conversions,
    conversion_rate
)
WITH historical_baseline AS (
    -- Get the LAST cumulative total for each agent from ANY month before the 12-month window
    -- This ensures continuity even if agents have gaps in their data
    SELECT
        customer_agent,
        total_customers AS baseline_total
    FROM monthly_agent_performance
    WHERE month < DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m')
      AND (customer_agent, month) IN (
        SELECT customer_agent, MAX(month)
        FROM monthly_agent_performance
        WHERE month < DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m')
        GROUP BY customer_agent
      )
),
monthly_certifications AS (
    -- Aggregate customers by agent and month for the last 12 months
    SELECT
        c.customer_agent,
        DATE_FORMAT(c.certified_at, '%Y-%m') AS month,
        COUNT(*) AS customers_certified_this_month
    FROM customers c
    WHERE c.customer_agent IS NOT NULL
      AND c.current_status = 'CERTIFIED'
      AND c.certified_at IS NOT NULL
      AND c.certified_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH)
      AND c.deleted_at IS NULL
    GROUP BY c.customer_agent, DATE_FORMAT(c.certified_at, '%Y-%m')
),
with_baseline AS (
    -- Join monthly data with historical baseline
    -- Use COLLATE to handle different character set collations between tables
    SELECT
        mc.customer_agent,
        mc.month,
        mc.customers_certified_this_month,
        COALESCE(hb.baseline_total, 0) AS baseline_total
    FROM monthly_certifications mc
    LEFT JOIN historical_baseline hb ON mc.customer_agent COLLATE utf8mb4_unicode_ci = hb.customer_agent COLLATE utf8mb4_unicode_ci
),
cumulative_totals AS (
    -- Calculate cumulative totals using window function, starting from baseline
    SELECT
        customer_agent,
        month,
        customers_certified_this_month AS new_customers,
        customers_certified_this_month AS conversions,
        baseline_total + SUM(customers_certified_this_month) OVER (
            PARTITION BY customer_agent
            ORDER BY month
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS total_customers
    FROM with_baseline
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

-- Step 3: Verification - Check updated data for recent months
-- Verify that the data was inserted correctly
SELECT
    customer_agent,
    month,
    total_customers,
    new_customers,
    conversions,
    conversion_rate,
    updated_at
FROM monthly_agent_performance
WHERE month >= DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m')
ORDER BY month DESC, customer_agent
LIMIT 50;

-- Step 4: Summary statistics for last 12 months
-- Shows overall statistics about the updated data
SELECT
    COUNT(*) AS total_records_updated,
    SUM(total_customers) AS sum_total_customers,
    SUM(new_customers) AS sum_new_customers,
    SUM(conversions) AS sum_conversions,
    AVG(conversion_rate) AS avg_conversion_rate
FROM monthly_agent_performance
WHERE month >= DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m');

-- Step 5: Top performers in the last 12 months
-- Shows agents ranked by total conversions in recent period
SELECT
    customer_agent,
    SUM(conversions) AS total_conversions_last_12_months,
    COUNT(DISTINCT month) AS active_months,
    ROUND(AVG(conversion_rate), 2) AS avg_conversion_rate
FROM monthly_agent_performance
WHERE month >= DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m')
GROUP BY customer_agent
ORDER BY total_conversions_last_12_months DESC
LIMIT 10;
