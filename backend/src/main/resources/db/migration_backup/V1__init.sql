-- Schema initialization based on PLAN.md
CREATE TYPE customer_status AS ENUM (
  'Customer called',
  'Replied to customer',
  'Order placed',
  'Product delivered',
  'Business done',
  'Lost'
);

CREATE TABLE customers (
  id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  phone TEXT NOT NULL,
  company TEXT,
  business_requirements TEXT,
  business_type TEXT,
  age INTEGER,
  education TEXT,
  gender TEXT,
  location TEXT,
  current_status customer_status NOT NULL DEFAULT 'Customer called',
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now(),
  deleted_at TIMESTAMPTZ,
  UNIQUE (phone)
);

CREATE TABLE status_history (
  id UUID PRIMARY KEY,
  customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
  from_status customer_status,
  to_status customer_status NOT NULL,
  reason TEXT,
  changed_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_customers_not_deleted_updated ON customers(updated_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_name ON customers(LOWER(name)) WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_status_history_customer_time ON status_history(customer_id, changed_at DESC);
