-- ============================================================================
-- Backfill Script for monthly_certified_count_by_certificate_type Table
-- ============================================================================
-- This script aggregates historical certification data from the customers table
-- and populates the monthly_certified_count_by_certificate_type table.
--
-- IMPORTANT: This script will TRUNCATE the table before backfilling to ensure
-- a clean slate. Run this only once after deploying the V11 migration.
-- ============================================================================

-- Step 1: Preview existing certified customers by certificate type (DRY RUN)
-- This shows you what data will be backfilled before actually inserting it.
SELECT
    DATE_FORMAT(certified_at, '%Y-%m') AS month,
    certificate_type,
    COUNT(*) AS count
FROM customers
WHERE certified_at IS NOT NULL
--  AND current_status = 'CERTIFIED'
  AND deleted_at IS NULL
GROUP BY DATE_FORMAT(certified_at, '%Y-%m'), certificate_type
ORDER BY month DESC, certificate_type
LIMIT 20;

-- Step 2: EMPTY the monthly_certified_count_by_certificate_type table (FRESH START)
TRUNCATE TABLE monthly_certified_count_by_certificate_type;

-- Step 3: Insert aggregated data (BACKFILL)
-- Aggregates certified customers by month and certificate type
INSERT INTO monthly_certified_count_by_certificate_type (month, certificate_type, certified_count)
SELECT
    DATE_FORMAT(certified_at, '%Y-%m') AS month,
    certificate_type AS certificate_type,
    COUNT(*) AS certified_count
FROM customers
WHERE certified_at IS NOT NULL
--  AND current_status = 'CERTIFIED'
  AND deleted_at IS NULL
GROUP BY DATE_FORMAT(certified_at, '%Y-%m'), certificate_type
ORDER BY month, certificate_type;

-- Step 4: Verify the backfill data
-- Shows the inserted data
SELECT
    month,
    certificate_type,
    certified_count,
    created_at,
    updated_at
FROM monthly_certified_count_by_certificate_type
ORDER BY month DESC, certificate_type
LIMIT 20;

-- Step 5: Summary statistics
-- Shows total counts by certificate type
SELECT
    certificate_type,
    SUM(certified_count) AS total_certifications,
    COUNT(DISTINCT month) AS number_of_months
FROM monthly_certified_count_by_certificate_type
GROUP BY certificate_type
ORDER BY total_certifications DESC;
