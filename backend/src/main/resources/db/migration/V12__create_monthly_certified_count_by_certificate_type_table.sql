-- Create table for tracking monthly certified customer counts by certificate type
-- This table provides fast queries for certificate type trend charts

CREATE TABLE monthly_certified_count_by_certificate_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Auto-increment primary key',
    month VARCHAR(7) NOT NULL COMMENT 'Month in yyyy-MM format (e.g., 2024-01)',
    certificate_type VARCHAR(50) NOT NULL COMMENT 'Certificate type (ISO_9001, ISO_27001, ISO_14001, etc.)',
    certified_count INTEGER NOT NULL DEFAULT 0 COMMENT 'Number of customers certified in this month for this certificate type',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_month_certificate_type (month, certificate_type),
    CONSTRAINT valid_month_format_cert CHECK (month REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])'),
    CONSTRAINT non_negative_count_cert CHECK (certified_count >= 0)
) COMMENT='Monthly count of customers who reached CERTIFIED status, grouped by certificate type';

-- Create index for faster queries by certificate type
CREATE INDEX idx_cert_type_month ON monthly_certified_count_by_certificate_type(certificate_type, month);
