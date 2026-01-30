-- Remove NOT NULL constraints from customer_staging table to allow more flexible data storage

-- Remove NOT NULL from name column
ALTER TABLE customer_staging MODIFY name VARCHAR(255) NULL;

-- Remove NOT NULL from phone column
ALTER TABLE customer_staging MODIFY phone VARCHAR(255) NULL;

-- Remove NOT NULL from current_status column
ALTER TABLE customer_staging MODIFY current_status VARCHAR(50) NULL DEFAULT 'NEW';

-- Remove NOT NULL from import_status column
ALTER TABLE customer_staging MODIFY import_status VARCHAR(50) NULL DEFAULT 'PENDING';

-- Remove NOT NULL from created_at column
ALTER TABLE customer_staging MODIFY created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP;
