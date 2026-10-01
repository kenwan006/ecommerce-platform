"""Index internal documents into PostgreSQL with pgvector."""

import argparse
import asyncio
from pathlib import Path

from app.config import settings
from app.rag import build_index


def main() -> None:
    parser = argparse.ArgumentParser(description="Index internal documents into PostgreSQL.")
    parser.add_argument("--documents", type=Path, default=settings.rag_document_dir)
    parser.add_argument("--database-url", default=settings.rag_database_url)
    parser.add_argument("--model", default=settings.rag_embedding_model)
    args = parser.parse_args()
    chunks = asyncio.run(
        build_index(
            document_dir=args.documents,
            database_url=args.database_url,
            embedding_model=args.model,
        )
    )
    print(f"Indexed {chunks} chunks from {args.documents} into PostgreSQL.")


if __name__ == "__main__":
    main()
