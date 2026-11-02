-- User-scoped agent memory. Apply after 001_rag.sql.
-- UUIDs are created by the application so this does not require pgcrypto.

CREATE TABLE IF NOT EXISTS conversations (
    id UUID PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    title TEXT,
    summary TEXT NOT NULL DEFAULT '',
    summary_through_message_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    archived_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS conversations_by_user
    ON conversations (tenant_id, user_id, updated_at DESC)
    WHERE archived_at IS NULL;

CREATE TABLE IF NOT EXISTS conversation_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('user', 'assistant')),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    token_count INTEGER
);

CREATE INDEX IF NOT EXISTS conversation_messages_by_conversation
    ON conversation_messages (conversation_id, created_at ASC);

CREATE INDEX IF NOT EXISTS conversation_messages_by_user
    ON conversation_messages (tenant_id, user_id, created_at DESC);

-- user_memories was created with the RAG schema. Extend it without modifying
-- the already-applied 001 migration.
ALTER TABLE user_memories
    ADD COLUMN IF NOT EXISTS source_conversation_id UUID,
    ADD COLUMN IF NOT EXISTS source_message_id UUID,
    ADD COLUMN IF NOT EXISTS confidence NUMERIC(4, 3),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE user_memories
    DROP CONSTRAINT IF EXISTS user_memories_status_check;

ALTER TABLE user_memories
    ADD CONSTRAINT user_memories_status_check
    CHECK (status IN ('active', 'superseded', 'deleted'));

CREATE INDEX IF NOT EXISTS user_memories_active_by_user_type
    ON user_memories (tenant_id, user_id, memory_type, updated_at DESC)
    WHERE status = 'active';
