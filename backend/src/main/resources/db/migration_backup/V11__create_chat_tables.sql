-- Create chat sessions table
CREATE TABLE chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    participant1_phone VARCHAR(20) NOT NULL,
    participant2_phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP,
    last_message_preview VARCHAR(200),
    
    -- Ensure participants are different
    CONSTRAINT check_different_participants CHECK (participant1_phone != participant2_phone),
    
    -- Foreign key constraints to sales table
    CONSTRAINT fk_chat_session_participant1 FOREIGN KEY (participant1_phone) REFERENCES sales(phone),
    CONSTRAINT fk_chat_session_participant2 FOREIGN KEY (participant2_phone) REFERENCES sales(phone)
);

-- Create chat messages table
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    chat_session_id BIGINT NOT NULL,
    sender_phone VARCHAR(20) NOT NULL,
    message_content TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    
    -- Foreign key constraints
    CONSTRAINT fk_chat_message_session FOREIGN KEY (chat_session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_phone) REFERENCES sales(phone)
);

-- Create indexes for better performance
CREATE INDEX idx_chat_sessions_participant1 ON chat_sessions(participant1_phone);
CREATE INDEX idx_chat_sessions_participant2 ON chat_sessions(participant2_phone);
CREATE INDEX idx_chat_sessions_last_message ON chat_sessions(last_message_at DESC);
CREATE INDEX idx_chat_messages_session ON chat_messages(chat_session_id);
CREATE INDEX idx_chat_messages_sender ON chat_messages(sender_phone);
CREATE INDEX idx_chat_messages_sent_at ON chat_messages(sent_at);

-- Create unique constraint to prevent duplicate sessions between same participants
CREATE UNIQUE INDEX idx_chat_sessions_unique_participants 
ON chat_sessions(LEAST(participant1_phone, participant2_phone), GREATEST(participant1_phone, participant2_phone));

-- Create trigger to update chat_session updated_at and last_message fields
CREATE OR REPLACE FUNCTION update_chat_session_on_message()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE chat_sessions 
    SET 
        updated_at = CURRENT_TIMESTAMP,
        last_message_at = NEW.sent_at,
        last_message_preview = CASE 
            WHEN LENGTH(NEW.message_content) > 200 
            THEN LEFT(NEW.message_content, 197) || '...'
            ELSE NEW.message_content
        END
    WHERE id = NEW.chat_session_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_chat_session_on_message
    AFTER INSERT ON chat_messages
    FOR EACH ROW
    EXECUTE FUNCTION update_chat_session_on_message();