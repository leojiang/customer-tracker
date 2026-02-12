-- Add performance indexes for common query patterns
-- This migration addresses improvement #8 from the improvement plan

-- Composite index for customers filtered by sales person and ordered by creation time
-- Used in: Customer search with sales_phone filter, ordered by createdAt
CREATE INDEX idx_customer_sales_created
  ON customers(sales_phone, created_at DESC);

-- Composite index for customers filtered by status and certified date
-- Used in: Analytics queries for certified customers, trend analysis
CREATE INDEX idx_customer_status_certified
  ON customers(current_status, certified_at);

-- Composite index for customers filtered by type and creation time
-- Used in: Customer type analytics and reporting
CREATE INDEX idx_customer_type_created
  ON customers(customer_type, created_at DESC);

-- Index on status_history for querying by customer and date
-- Used in: Status history lookups, audit trails
CREATE INDEX idx_status_history_customer_date
  ON status_history(customer_id, changed_at DESC);

-- Index on status_history for daily trend analysis grouped by user
-- Used in: Analytics - daily status change trends
CREATE INDEX idx_status_history_user_date
  ON status_history(changed_by, changed_at DESC);

-- Index for customer_agent lookups (common filter in dashboard)
CREATE INDEX idx_customer_agent
  ON customers(customer_agent);

-- Index for certificate issuer searches
CREATE INDEX idx_customer_certificate_issuer
  ON customers(certificate_issuer);

-- Index for combined id_card and certificate_type lookups
-- Note: Unique constraint already exists, this index improves query performance
CREATE INDEX idx_customer_idcard_certtype
  ON customers(id_card, certificate_type);
