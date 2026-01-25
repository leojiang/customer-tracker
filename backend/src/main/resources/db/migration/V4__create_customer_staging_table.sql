-- Create customer_staging table for import workflow (MySQL Version)
-- This table temporarily stores uploaded customer data before user confirms import

CREATE TABLE customer_staging (
    id BINARY(16) PRIMARY KEY,
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
    certified_at DATETIME,
    import_status VARCHAR(50) NOT NULL DEFAULT 'PENDING' COMMENT 'Import status: PENDING, VALID (new), UPDATE (update existing), DUPLICATE, INVALID',
    validation_message TEXT COMMENT 'Validation message explaining why a record is invalid or duplicate',
    excel_row_number INTEGER COMMENT 'Row number from the Excel file for reference',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='Staging table for customer import workflow - stores data temporarily before user confirms import';

-- Create index on phone for faster duplicate checking
CREATE INDEX idx_customer_staging_phone ON customer_staging(phone);

-- Create index on certificate_type for filtering
CREATE INDEX idx_customer_staging_certificate_type ON customer_staging(certificate_type);

-- Create index on import_status for filtering during review
CREATE INDEX idx_customer_staging_import_status ON customer_staging(import_status);
