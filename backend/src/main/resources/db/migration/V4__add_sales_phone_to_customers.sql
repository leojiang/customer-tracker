-- Add sales_phone column to customers table
ALTER TABLE customers ADD COLUMN sales_phone VARCHAR(20);

-- Create index for sales filtering
CREATE INDEX idx_customers_sales_phone ON customers(sales_phone) WHERE deleted_at IS NULL;