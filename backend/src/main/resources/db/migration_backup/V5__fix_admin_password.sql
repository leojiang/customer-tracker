-- Fix admin user password with correct BCrypt hash for '123456'
UPDATE sales 
SET password = '$2a$10$4KzCUhnd9TmmlpjKAvlsSegh0jvLsq5BEaognpyp/6thn1nbTUOAO' 
WHERE phone = '18980994001';