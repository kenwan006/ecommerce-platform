import asyncio
import json
from pathlib import Path
from types import SimpleNamespace

from app import rag


class FakeEmbeddings:
    async def create(self, *, model: str, input: str):
        vectors = {
            "How long do customers have to return an item?": [1.0, 0.0],
            "What is the CEO's home address?": [0.0, 1.0],
        }
        return SimpleNamespace(data=[SimpleNamespace(embedding=vectors[input])])


class FakeCursor:
    def __init__(self, rows_by_vector: dict[str, list[tuple[str, int, float, str]]]):
        self.rows_by_vector = rows_by_vector
        self.rows: list[tuple[str, int, float, str]] = []

    async def __aenter__(self):
        return self

    async def __aexit__(self, *_):
        return None

    async def execute(self, _sql: str, parameters: tuple[str, str, int]):
        self.rows = self.rows_by_vector[parameters[0]]

    async def fetchall(self):
        return self.rows


class FakeConnection:
    def __init__(self, rows_by_vector: dict[str, list[tuple[str, int, float, str]]]):
        self.cursor_instance = FakeCursor(rows_by_vector)

    async def __aenter__(self):
        return self

    async def __aexit__(self, *_):
        return None

    def cursor(self):
        return self.cursor_instance


class FakeOpenAIClient:
    embeddings = FakeEmbeddings()


def test_chunk_text_preserves_content_and_uses_overlap():
    text = "alpha " * 400

    chunks = rag.chunk_text(text, chunk_size=100, overlap=20)

    assert len(chunks) > 1
    assert chunks[0].endswith("alpha")
    assert chunks[1].startswith("alpha")
    assert all(len(chunk) <= 100 for chunk in chunks)


def test_content_hash_is_stable_and_changes_with_content():
    assert rag.content_hash("policy") == rag.content_hash("policy")
    assert rag.content_hash("policy") != rag.content_hash("different policy")


def test_to_vector_literal_uses_pgvector_format():
    assert rag.to_vector_literal([1.0, -0.5]) == "[1.0,-0.5]"


def test_golden_retrieval_cases(monkeypatch):
    fixture = Path(__file__).parent / "fixtures" / "rag_eval_cases.json"
    cases = json.loads(fixture.read_text(encoding="utf-8"))
    rows_by_vector = {
        "[1.0,0.0]": [
            ("returns-policy.md", 0, 0.92, "Items may be returned within 30 days."),
            ("shipping-guide.md", 1, 0.31, "Shipping guidance."),
        ],
        "[0.0,1.0]": [("returns-policy.md", 0, 0.08, "Unrelated policy text.")],
    }

    async def connect(_database_url: str):
        return FakeConnection(rows_by_vector)

    monkeypatch.setattr(rag.AsyncConnection, "connect", connect)
    retrieval = rag.PostgresRag(
        database_url="postgresql://unused",
        embedding_model="fake-embedding-model",
        top_k=4,
        min_score=0.25,
        client=FakeOpenAIClient(),
    )

    for case in cases:
        matches = asyncio.run(retrieval.search(case["question"]))
        sources = {match.source for match in matches}
        if case["should_abstain"]:
            assert matches == []
        else:
            assert case["expected_source"] in sources
