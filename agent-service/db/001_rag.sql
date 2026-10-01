CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGSERIAL PRIMARY KEY,
    document_key TEXT NOT NULL,
    title TEXT NOT NULL,
    source_filename TEXT NOT NULL,
    version INTEGER NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('indexing', 'active', 'superseded', 'failed')),
    effective_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_key, version)
);

CREATE UNIQUE INDEX IF NOT EXISTS knowledge_documents_one_active_version
    ON knowledge_documents (document_key)
    WHERE status = 'active';

CREATE TABLE IF NOT EXISTS knowledge_document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    chunk_number INTEGER NOT NULL,
    content_hash TEXT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_id, chunk_number)
);

CREATE INDEX IF NOT EXISTS knowledge_document_chunks_embedding_hnsw
    ON knowledge_document_chunks USING hnsw (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS user_memories (
    id BIGSERIAL PRIMARY KEY,
    tenant_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    memory_type TEXT NOT NULL,
    memory_text TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'deleted')),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS user_memories_lookup
    ON user_memories (tenant_id, user_id, status, expires_at);

CREATE INDEX IF NOT EXISTS user_memories_embedding_hnsw
    ON user_memories USING hnsw (embedding vector_cosine_ops);
