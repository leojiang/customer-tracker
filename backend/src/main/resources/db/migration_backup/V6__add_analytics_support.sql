-- Add analytics support and performance optimizations for dashboard
-- This migration focuses on MVP features using existing data with performance enhancements

-- Update existing customers to be associated with admin user for initial data
UPDATE customers SET sales_phone = '18980994001' WHERE sales_phone IS NULL;

-- Add foreign key constraint
ALTER TABLE customers ADD CONSTRAINT fk_customers_sales FOREIGN KEY (sales_phone) REFERENCES sales(phone);
CREATE INDEX idx_customers_created_at ON customers(created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_status ON customers(current_status) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_status_sales ON customers(current_status, sales_phone) WHERE deleted_at IS NULL;

-- Add status_history performance indexes for analytics queries
CREATE INDEX idx_status_history_changed_at ON status_history(changed_at DESC);
CREATE INDEX idx_status_history_to_status ON status_history(to_status, changed_at DESC);

-- Create analytics snapshots table for performance optimization (future use)
CREATE TABLE analytics_snapshots (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  snapshot_date DATE NOT NULL,
  sales_phone VARCHAR(20), -- NULL for system-wide snapshots
  metric_type VARCHAR(50) NOT NULL, -- 'daily', 'weekly', 'monthly'
  
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
  UNIQUE(snapshot_date, sales_phone, metric_type)
);

-- Indexes for analytics snapshots
CREATE INDEX idx_analytics_snapshots_date_sales ON analytics_snapshots(snapshot_date, sales_phone);
CREATE INDEX idx_analytics_snapshots_type ON analytics_snapshots(metric_type);

-- Update status_history to include sales_phone for better analytics performance
ALTER TABLE status_history ADD COLUMN sales_phone VARCHAR(20);

-- Populate sales_phone in status_history from customers table
UPDATE status_history 
SET sales_phone = c.sales_phone 
FROM customers c 
WHERE status_history.customer_id = c.id;

-- Add index for status_history analytics queries
CREATE INDEX idx_status_history_sales_date ON status_history(sales_phone, changed_at DESC);

-- Add comment for analytics tables
COMMENT ON TABLE analytics_snapshots IS 'Pre-aggregated analytics data for dashboard performance optimization';
COMMENT ON COLUMN analytics_snapshots.sales_phone IS 'NULL for system-wide metrics, specific phone for sales-specific metrics';
COMMENT ON COLUMN analytics_snapshots.metric_type IS 'Aggregation period: daily, weekly, monthly';