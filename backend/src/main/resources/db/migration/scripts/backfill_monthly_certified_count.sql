-- ============================================================================
-- BACKFILL MONTHLY CERTIFIED COUNT
-- ============================================================================
-- Purpose: Aggregate existing certified customers by month
-- Usage: Run this script manually AFTER deploying the database migration
-- IMPORTANT: This script will EMPTY the monthly_certified_count table before backfilling
-- ============================================================================

-- Step 1: Preview existing certified customers (DRY RUN)
-- This shows you what data will be inserted before actually inserting it
SELECT
    DATE_FORMAT(certified_at, '%Y-%m') AS month,
    COUNT(*) AS count,
    SUM(CASE WHEN customer_type = 'NEW_CUSTOMER' THEN 1 ELSE 0 END) AS new_customer_count,
    SUM(CASE WHEN customer_type = 'RENEW_CUSTOMER' THEN 1 ELSE 0 END) AS renew_customer_count
FROM customers
WHERE certified_at IS NOT NULL
--  AND current_status = 'CERTIFIED'
  AND deleted_at IS NULL
GROUP BY DATE_FORMAT(certified_at, '%Y-%m')
ORDER BY month DESC
LIMIT 20;

-- Step 2: EMPTY the monthly_certified_count table (FRESH START)
-- WARNING: This will DELETE all existing data in the table!
TRUNCATE TABLE monthly_certified_count;

-- Step 3: Insert aggregated data (BACKFILL)
-- This aggregates all existing certified customers by month with customer type breakdown
INSERT INTO monthly_certified_count (month, certified_count, new_customer_certified_count, renew_customer_certified_count)
SELECT
    DATE_FORMAT(certified_at, '%Y-%m') AS month,
    COUNT(*) AS certified_count,
    SUM(CASE WHEN customer_type = 'NEW_CUSTOMER' THEN 1 ELSE 0 END) AS new_customer_certified_count,
    SUM(CASE WHEN customer_type = 'RENEW_CUSTOMER' THEN 1 ELSE 0 END) AS renew_customer_certified_count
FROM customers
WHERE certified_at IS NOT NULL
--  AND current_status = 'CERTIFIED'
  AND deleted_at IS NULL
GROUP BY DATE_FORMAT(certified_at, '%Y-%m')
ORDER BY month;

-- Step 4: Verification - Check inserted data
-- Verify that the data was inserted correctly
SELECT
    month,
    certified_count,
    new_customer_certified_count,
    renew_customer_certified_count,
    created_at,
    updated_at
FROM monthly_certified_count
ORDER BY month DESC
LIMIT 20;

-- Step 5: Summary statistics
-- Shows overall statistics about the backfilled data
SELECT
    COUNT(*) AS total_months,
    SUM(certified_count) AS total_certified,
    SUM(new_customer_certified_count) AS total_new_customers,
    SUM(renew_customer_certified_count) AS total_renew_customers,
    MIN(month) AS earliest_month,
    MAX(month) AS latest_month
FROM monthly_certified_count;

-- Step 6: Data integrity check - verify counts match customer table
-- This should return no rows if data is consistent
-- If any rows are returned, there's a data mismatch that needs investigation
SELECT
    mcc.month,
    mcc.certified_count AS table_count,
    COUNT(c.id) AS actual_count,
    (mcc.certified_count - COUNT(c.id)) AS difference,
    mcc.new_customer_certified_count AS table_new_count,
    SUM(CASE WHEN c.customer_type = 'NEW_CUSTOMER' THEN 1 ELSE 0 END) AS actual_new_count,
    mcc.renew_customer_certified_count AS table_renew_count,
    SUM(CASE WHEN c.customer_type = 'RENEW_CUSTOMER' THEN 1 ELSE 0 END) AS actual_renew_count
FROM monthly_certified_count mcc
LEFT JOIN customers c ON DATE_FORMAT(c.certified_at, '%Y-%m') = mcc.month
    AND c.current_status = 'CERTIFIED'
    AND c.deleted_at IS NULL
GROUP BY mcc.month, mcc.certified_count, mcc.new_customer_certified_count, mcc.renew_customer_certified_count
HAVING table_count != actual_count
    OR table_new_count != SUM(CASE WHEN c.customer_type = 'NEW_CUSTOMER' THEN 1 ELSE 0 END)
    OR table_renew_count != SUM(CASE WHEN c.customer_type = 'RENEW_CUSTOMER' THEN 1 ELSE 0 END);
