import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()


@dataclass(frozen=True)
class Settings:
    openai_api_key: str
    spring_api_url: str
    openai_docs_mcp_url: str
    github_token: str
    github_mcp_url: str
    rag_document_dir: Path
    rag_embedding_model: str
    rag_top_k: int
    rag_min_score: float
    rag_database_url: str


settings = Settings(
    openai_api_key=os.getenv("OPENAI_API_KEY", ""),
    spring_api_url=os.getenv("SPRING_API_URL", "http://localhost:8080"),
    openai_docs_mcp_url=os.getenv("OPENAI_DOCS_MCP_URL", "https://developers.openai.com/mcp"),
    github_token=os.getenv("GITHUB_TOKEN", ""),
    github_mcp_url=os.getenv("GITHUB_MCP_URL", "https://api.githubcopilot.com/mcp/"),
    rag_document_dir=Path(os.getenv("RAG_DOCUMENT_DIR", "data/rag/documents")),
    rag_embedding_model=os.getenv("RAG_EMBEDDING_MODEL", "text-embedding-3-small"),
    rag_top_k=int(os.getenv("RAG_TOP_K", "4")),
    rag_min_score=float(os.getenv("RAG_MIN_SCORE", "0.25")),
    rag_database_url=os.getenv("RAG_DATABASE_URL", "postgresql://chaowan@127.0.0.1:5432/postgres"),
)
