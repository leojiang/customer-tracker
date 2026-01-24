-- Create customer_staging table for import workflow
-- This table temporarily stores uploaded customer data before user confirms import

CREATE TABLE customer_staging (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    certificate_issuer VARCHAR(255),
    business_requirements TEXT,
    certificate_type VARCHAR(100),
    age INTEGER,
    education VARCHAR(50),
    gender VARCHAR(50),
    address VARCHAR(500),
    id_card VARCHAR(255),
    current_status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    customer_agent VARCHAR(255),
    certified_at TIMESTAMP WITH TIME ZONE,
    import_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    validation_message TEXT,
    row_number INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on phone for faster duplicate checking
CREATE INDEX idx_customer_staging_phone ON customer_staging(phone);

-- Create index on certificate_type for filtering
CREATE INDEX idx_customer_staging_certificate_type ON customer_staging(certificate_type);

-- Create index on import_status for filtering during review
CREATE INDEX idx_customer_staging_import_status ON customer_staging(import_status);

-- Add comment
COMMENT ON TABLE customer_staging IS 'Staging table for customer import workflow - stores data temporarily before user confirms import';
COMMENT ON COLUMN customer_staging.import_status IS 'Import status: PENDING, VALID (new), UPDATE (update existing), DUPLICATE, INVALID';
COMMENT ON COLUMN customer_staging.validation_message IS 'Validation message explaining why a record is invalid or duplicate';
COMMENT ON COLUMN customer_staging.row_number IS 'Row number from the Excel file for reference';
