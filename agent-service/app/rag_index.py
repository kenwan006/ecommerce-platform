"""Build the local RAG index from documents in data/rag/documents/."""

import argparse
import asyncio
from pathlib import Path

from app.config import settings
from app.rag import build_index


def main() -> None:
    parser = argparse.ArgumentParser(description="Build the internal-document RAG index.")
    parser.add_argument("--documents", type=Path, default=settings.rag_document_dir)
    parser.add_argument("--output", type=Path, default=settings.rag_index_path)
    parser.add_argument("--model", default=settings.rag_embedding_model)
    args = parser.parse_args()
    chunks = asyncio.run(build_index(document_dir=args.documents, index_path=args.output, embedding_model=args.model))
    print(f"Indexed {chunks} chunks from {args.documents} into {args.output}.")


if __name__ == "__main__":
    main()
