-- Add name column to sales table
ALTER TABLE sales ADD COLUMN name VARCHAR(20) NOT NULL DEFAULT '';

-- Update the hardcoded admin user's name
UPDATE sales SET name = '洪晓英' WHERE phone = '18980994196';
