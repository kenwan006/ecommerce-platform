"""PostgreSQL/pgvector retrieval for internal documentation."""

from __future__ import annotations

import hashlib
from dataclasses import dataclass
from pathlib import Path

from openai import AsyncOpenAI
from psycopg import AsyncConnection


SUPPORTED_SUFFIXES = {".md", ".txt"}


@dataclass(frozen=True)
class SearchResult:
    source: str
    chunk: int
    score: float
    text: str


def chunk_text(text: str, *, chunk_size: int = 1400, overlap: int = 200) -> list[str]:
    """Split text into overlapping, readable chunks without splitting words."""
    normalized = "\n".join(line.rstrip() for line in text.splitlines()).strip()
    if not normalized:
        return []

    chunks: list[str] = []
    start = 0
    while start < len(normalized):
        end = min(start + chunk_size, len(normalized))
        if end < len(normalized):
            boundary = max(normalized.rfind("\n", start, end), normalized.rfind(" ", start, end))
            if boundary > start + chunk_size // 2:
                end = boundary
        chunk = normalized[start:end].strip()
        if chunk:
            chunks.append(chunk)
        if end == len(normalized):
            break
        start = max(end - overlap, start + 1)
    return chunks


class PostgresRag:
    def __init__(
        self,
        *,
        database_url: str,
        embedding_model: str,
        top_k: int,
        min_score: float,
        client: AsyncOpenAI | None = None,
    ):
        self.database_url = database_url
        self.embedding_model = embedding_model
        if top_k < 1:
            raise ValueError("RAG_TOP_K must be at least 1.")
        self.top_k = top_k
        self.min_score = min_score
        self.client = client or AsyncOpenAI()
    async def search(self, query: str) -> list[SearchResult]:
        response = await self.client.embeddings.create(model=self.embedding_model, input=query)
        query_embedding = response.data[0].embedding
        vector = to_vector_literal(query_embedding)
        async with await AsyncConnection.connect(self.database_url) as connection:
            async with connection.cursor() as cursor:
                await cursor.execute(
                    """
                    SELECT document.source_filename, chunk.chunk_number,
                           1 - (chunk.embedding <=> %s::vector) AS score,
                           chunk.chunk_text
                    FROM knowledge_document_chunks AS chunk
                    JOIN knowledge_documents AS document ON document.id = chunk.document_id
                    WHERE document.status = 'active'
                      AND document.effective_at <= now()
                    ORDER BY chunk.embedding <=> %s::vector
                    LIMIT %s
                    """,
                    (vector, vector, self.top_k),
                )
                rows = await cursor.fetchall()
        return [
            SearchResult(source=source, chunk=chunk, score=float(score), text=text)
            for source, chunk, score, text in rows
            if score >= self.min_score
        ]


async def embed_texts(client: AsyncOpenAI, model: str, texts: list[str]) -> list[list[float]]:
    """Embed texts in API-sized batches, retaining the original ordering."""
    embeddings: list[list[float]] = []
    for start in range(0, len(texts), 100):
        response = await client.embeddings.create(model=model, input=texts[start : start + 100])
        embeddings.extend(item.embedding for item in response.data)
    return embeddings


async def build_index(
    *,
    document_dir: Path,
    database_url: str,
    embedding_model: str,
    client: AsyncOpenAI | None = None,
) -> int:
    if not document_dir.exists():
        raise FileNotFoundError(f"RAG document directory does not exist: {document_dir}")
    files = sorted(
        path
        for path in document_dir.rglob("*")
        if path.is_file() and path.suffix.lower() in SUPPORTED_SUFFIXES
    )
    documents = {
        path.relative_to(document_dir).as_posix(): chunk_text(path.read_text(encoding="utf-8"))
        for path in files
    }
    if not any(documents.values()):
        raise ValueError(f"No .md or .txt documents found under {document_dir}")

    openai_client = client or AsyncOpenAI()
    indexed_chunks = 0
    async with await AsyncConnection.connect(database_url) as connection:
        for source, chunks in documents.items():
            if not chunks:
                continue
            embeddings = await embed_texts(openai_client, embedding_model, chunks)
            document_key = Path(source).with_suffix("").as_posix()
            async with connection.transaction():
                async with connection.cursor() as cursor:
                    await cursor.execute("SELECT pg_advisory_xact_lock(hashtext(%s))", (document_key,))
                    await cursor.execute(
                        "SELECT COALESCE(MAX(version), 0) + 1 FROM knowledge_documents WHERE document_key = %s",
                        (document_key,),
                    )
                    version = (await cursor.fetchone())[0]
                    await cursor.execute(
                        """
                        INSERT INTO knowledge_documents
                            (document_key, title, source_filename, version, status)
                        VALUES (%s, %s, %s, %s, 'indexing')
                        RETURNING id
                        """,
                        (document_key, Path(source).stem, source, version),
                    )
                    document_id = (await cursor.fetchone())[0]
                    await cursor.executemany(
                        """
                        INSERT INTO knowledge_document_chunks
                            (document_id, chunk_number, content_hash, chunk_text, embedding)
                        VALUES (%s, %s, %s, %s, %s::vector)
                        """,
                        [
                            (document_id, number, content_hash(text), text, to_vector_literal(embedding))
                            for number, (text, embedding) in enumerate(zip(chunks, embeddings))
                        ],
                    )
                    await cursor.execute(
                        """
                        UPDATE knowledge_documents
                        SET status = 'superseded'
                        WHERE document_key = %s AND status = 'active'
                        """,
                        (document_key,),
                    )
                    await cursor.execute(
                        "UPDATE knowledge_documents SET status = 'active' WHERE id = %s",
                        (document_id,),
                    )
            indexed_chunks += len(chunks)
    return indexed_chunks


def content_hash(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def to_vector_literal(values: list[float]) -> str:
    return "[" + ",".join(str(value) for value in values) + "]"
