-- Add token_version column to sales table for single-session-per-user enforcement
-- This version increments on each login, invalidating previous JWT tokens

ALTER TABLE sales
ADD COLUMN token_version BIGINT DEFAULT 0 NOT NULL COMMENT 'Token version incremented on each login to invalidate previous sessions';
