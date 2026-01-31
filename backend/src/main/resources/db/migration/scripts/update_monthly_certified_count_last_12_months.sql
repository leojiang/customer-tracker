-- ============================================================================
-- UPDATE MONTHLY CERTIFIED COUNT - LAST 12 MONTHS ONLY
-- ============================================================================
-- Purpose: Update monthly_certified_count table with data from the last 12 months
-- Usage: Run this script after importing new customer data to refresh analytics
-- IMPORTANT: This script only affects the last 12 months, protecting historical data
-- ============================================================================

-- Step 1: Delete records from the last 12 months only
-- This preserves historical data older than 12 months
DELETE FROM monthly_certified_count
WHERE month >= DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m');

-- Step 2: Insert/Update data for the last 12 months
-- Uses REPLACE to handle both new and existing records
INSERT INTO monthly_certified_count (month, certified_count)
SELECT
    DATE_FORMAT(certified_at, '%Y-%m') AS month,
    COUNT(*) AS certified_count
FROM customers
WHERE certified_at IS NOT NULL
  AND c.current_status = 'CERTIFIED'
  AND certified_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH)
  AND deleted_at IS NULL
GROUP BY DATE_FORMAT(certified_at, '%Y-%m')
ORDER BY month;

-- Step 3: Verification - Check updated data
-- Verify that the data was inserted correctly for recent months
SELECT
    month,
    certified_count,
    updated_at
FROM monthly_certified_count
WHERE month >= DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m')
ORDER BY month DESC;

-- Step 4: Summary statistics for last 12 months
-- Shows overall statistics about the updated data
SELECT
    COUNT(*) AS total_months_updated,
    SUM(certified_count) AS total_certified_last_12_months,
    MIN(month) AS earliest_month_updated,
    MAX(month) AS latest_month_updated
FROM monthly_certified_count
WHERE month >= DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m');
