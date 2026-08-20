CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    repair_request_id BIGINT NULL REFERENCES repair_requests(id) ON DELETE SET NULL,
    conversation_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    closed_at TIMESTAMPTZ NULL,
    CONSTRAINT chk_conversations_type CHECK (conversation_type IN ('CUSTOMER_TECHNICIAN', 'TECHNICIAN_MANAGER')),
    CONSTRAINT chk_conversations_status CHECK (status IN ('ACTIVE', 'CLOSED'))
);

CREATE INDEX idx_conversations_repair_request_id ON conversations(repair_request_id);
CREATE INDEX idx_conversations_type ON conversations(conversation_type);
CREATE INDEX idx_conversations_updated_at ON conversations(updated_at DESC);
CREATE UNIQUE INDEX uq_conversations_request_type
    ON conversations(repair_request_id, conversation_type)
    WHERE repair_request_id IS NOT NULL;

CREATE TABLE conversation_participants (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    actor_type VARCHAR(32) NOT NULL,
    actor_id BIGINT NOT NULL,
    role VARCHAR(32) NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    left_at TIMESTAMPTZ NULL,
    last_read_message_id BIGINT NULL,
    last_read_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_conversation_participants_actor_type CHECK (actor_type IN ('STAFF', 'CUSTOMER', 'TECHNICIAN')),
    CONSTRAINT uq_conversation_participant UNIQUE (conversation_id, actor_type, actor_id)
);

CREATE INDEX idx_conversation_participants_actor ON conversation_participants(actor_type, actor_id);
CREATE INDEX idx_conversation_participants_conv ON conversation_participants(conversation_id);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_type VARCHAR(32) NOT NULL,
    sender_id BIGINT NOT NULL,
    client_message_id VARCHAR(64) NOT NULL,
    message_type VARCHAR(32) NOT NULL,
    text TEXT NULL,
    attachment_id BIGINT NULL REFERENCES repair_attachments(id) ON DELETE SET NULL,
    reply_to_message_id BIGINT NULL REFERENCES chat_messages(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    edited_at TIMESTAMPTZ NULL,
    deleted_at TIMESTAMPTZ NULL,
    CONSTRAINT chk_chat_messages_sender_type CHECK (sender_type IN ('STAFF', 'CUSTOMER', 'TECHNICIAN')),
    CONSTRAINT chk_chat_messages_message_type CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE')),
    CONSTRAINT uq_chat_message_client_msg UNIQUE (conversation_id, sender_type, sender_id, client_message_id)
);

CREATE INDEX idx_chat_messages_conv_id ON chat_messages(conversation_id, id);
CREATE INDEX idx_chat_messages_conv_created ON chat_messages(conversation_id, created_at);
CREATE INDEX idx_chat_messages_attachment ON chat_messages(attachment_id);

ALTER TABLE conversation_participants
    ADD CONSTRAINT fk_conversation_participants_last_read_message
    FOREIGN KEY (last_read_message_id)
    REFERENCES chat_messages(id)
    ON DELETE SET NULL;
