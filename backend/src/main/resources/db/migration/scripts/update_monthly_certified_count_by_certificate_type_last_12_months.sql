-- ============================================================================
-- UPDATE MONTHLY CERTIFIED COUNT BY CERTIFICATE TYPE - LAST 12 MONTHS ONLY
-- ============================================================================
-- Purpose: Update monthly_certified_count_by_certificate_type table with data from the last 12 months
-- Usage: Run this script after importing new customer data to refresh analytics
-- IMPORTANT: This script only affects the last 12 months, protecting historical data
-- ============================================================================

-- Step 1: Delete records from the last 12 months only
-- This preserves historical data older than 12 months
DELETE FROM monthly_certified_count_by_certificate_type
WHERE month >= DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m');

-- Step 2: Insert/Update data for the last 12 months
-- Inserts aggregated data by month and certificate type
INSERT INTO monthly_certified_count_by_certificate_type (month, certificate_type, certified_count)
SELECT
    DATE_FORMAT(certified_at, '%Y-%m') AS month,
    certificate_type AS certificate_type,
    COUNT(*) AS certified_count
FROM customers
WHERE certified_at IS NOT NULL
  AND current_status = 'CERTIFIED'
  AND certified_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH)
  AND deleted_at IS NULL
GROUP BY DATE_FORMAT(certified_at, '%Y-%m'), certificate_type
ORDER BY month, certificate_type;

-- Step 3: Verify the updated data
-- Shows the inserted data for recent months
SELECT
    month,
    certificate_type,
    certified_count,
    updated_at
FROM monthly_certified_count_by_certificate_type
WHERE month >= DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m')
ORDER BY month DESC, certificate_type
LIMIT 50;

-- Step 4: Summary statistics for last 12 months
-- Shows total counts by certificate type for recent period
SELECT
    certificate_type,
    SUM(certified_count) AS total_certifications_last_12_months,
    COUNT(DISTINCT month) AS number_of_months_updated
FROM monthly_certified_count_by_certificate_type
WHERE month >= DATE_FORMAT(DATE_SUB(CURRENT_DATE(), INTERVAL 12 MONTH), '%Y-%m')
GROUP BY certificate_type
ORDER BY total_certifications_last_12_months DESC;
