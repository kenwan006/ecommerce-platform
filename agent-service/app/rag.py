"""A small, file-backed retrieval layer for internal documentation.

The index is intentionally portable JSON so this service can start with a few
policy and guide files without requiring a separate vector database.
"""

from __future__ import annotations

import json
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from openai import AsyncOpenAI


SUPPORTED_SUFFIXES = {".md", ".txt"}
INDEX_VERSION = 1


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


def cosine_similarity(left: list[float], right: list[float]) -> float:
    if len(left) != len(right):
        raise ValueError("Embedding dimensions do not match. Rebuild the RAG index.")
    denominator = math.sqrt(sum(value * value for value in left)) * math.sqrt(
        sum(value * value for value in right)
    )
    return 0.0 if denominator == 0 else sum(a * b for a, b in zip(left, right)) / denominator


class LocalRag:
    def __init__(
        self,
        *,
        index_path: Path,
        embedding_model: str,
        top_k: int,
        min_score: float,
        client: AsyncOpenAI | None = None,
    ):
        self.index_path = index_path
        self.embedding_model = embedding_model
        if top_k < 1:
            raise ValueError("RAG_TOP_K must be at least 1.")
        self.top_k = top_k
        self.min_score = min_score
        self.client = client or AsyncOpenAI()
        self._index: dict[str, Any] | None = None

    def _load_index(self) -> dict[str, Any]:
        if self._index is not None:
            return self._index
        if not self.index_path.exists():
            self._index = {"chunks": []}
            return self._index
        with self.index_path.open(encoding="utf-8") as file:
            self._index = json.load(file)
        if self._index.get("version") != INDEX_VERSION:
            raise RuntimeError("Unsupported RAG index format. Rebuild the index.")
        if self._index.get("embedding_model") != self.embedding_model:
            raise RuntimeError("RAG embedding model changed. Rebuild the index.")
        return self._index

    async def search(self, query: str) -> list[SearchResult]:
        index = self._load_index()
        chunks = index.get("chunks", [])
        if not chunks:
            return []
        response = await self.client.embeddings.create(model=self.embedding_model, input=query)
        query_embedding = response.data[0].embedding
        results = [
            SearchResult(
                source=chunk["source"],
                chunk=chunk["chunk"],
                score=cosine_similarity(query_embedding, chunk["embedding"]),
                text=chunk["text"],
            )
            for chunk in chunks
        ]
        ranked = sorted(results, key=lambda item: item.score, reverse=True)[: self.top_k]
        return [result for result in ranked if result.score >= self.min_score]


async def embed_texts(client: AsyncOpenAI, model: str, texts: list[str]) -> list[list[float]]:
    """Embed texts in API-sized batches, retaining the original ordering."""
    embeddings: list[list[float]] = []
    for start in range(0, len(texts), 100):
        response = await client.embeddings.create(model=model, input=texts[start : start + 100])
        embeddings.extend(item.embedding for item in response.data)
    return embeddings


async def build_index(
    *, document_dir: Path, index_path: Path, embedding_model: str, client: AsyncOpenAI | None = None
) -> int:
    if not document_dir.exists():
        raise FileNotFoundError(f"RAG document directory does not exist: {document_dir}")
    files = sorted(
        path
        for path in document_dir.rglob("*")
        if path.is_file() and path.suffix.lower() in SUPPORTED_SUFFIXES
    )
    records = [
        {"source": path.relative_to(document_dir).as_posix(), "chunk": number, "text": text}
        for path in files
        for number, text in enumerate(chunk_text(path.read_text(encoding="utf-8")))
    ]
    if not records:
        raise ValueError(f"No .md or .txt documents found under {document_dir}")

    openai_client = client or AsyncOpenAI()
    embeddings = await embed_texts(openai_client, embedding_model, [record["text"] for record in records])
    for record, embedding in zip(records, embeddings):
        record["embedding"] = embedding

    payload = {"version": INDEX_VERSION, "embedding_model": embedding_model, "chunks": records}
    index_path.parent.mkdir(parents=True, exist_ok=True)
    temporary_path = index_path.with_suffix(index_path.suffix + ".tmp")
    temporary_path.write_text(json.dumps(payload, separators=(",", ":")), encoding="utf-8")
    temporary_path.replace(index_path)
    return len(records)
