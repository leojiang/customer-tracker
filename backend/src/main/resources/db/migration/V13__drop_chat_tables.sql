-- Drop chat-related tables
-- This migration removes all chat functionality from the database

-- Drop chat_messages table first (due to foreign key constraint)
DROP TABLE IF EXISTS chat_messages CASCADE;

-- Drop chat_sessions table
DROP TABLE IF EXISTS chat_sessions CASCADE;
