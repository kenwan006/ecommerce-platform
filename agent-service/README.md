# Ecommerce agent service

## Role and startup

This FastAPI service runs separately from the Spring services on `http://localhost:8000`. React sends chat requests to `POST /api/ai/ask`; the agent forwards the customer's bearer JWT only when an order tool needs Commerce data.

```bash
source .venv/bin/activate
uvicorn app.main:app --reload --port 8000
```

It is not an order, payment, or inventory system of record. Commerce (`8080`) owns customer/order data; Warehouse (`8082`) owns stock and fulfillment; Payment (`8083`) owns Stripe payment processing. See the root [README](../README.md) for the complete local startup sequence.

## Internal-document RAG

Place approved internal policy and guide documents as UTF-8 `.md` or `.txt`
files under `data/rag/documents/` (subdirectories are supported), then build
the local index:

```bash
python3 -m app.rag_index
```

The index is written to `data/rag/index.json` and is intentionally ignored by
Git because it contains embedded copies of internal content. Re-run the command
whenever a source document changes, and deploy the resulting index alongside
the service.

At runtime, the agent uses `search_internal_docs` only for internal policy and
guide questions, retrieves the most relevant chunks, and cites source filenames
in its response. If no index exists or nothing meets the relevance threshold,
it does not invent policy. Configuration is available through environment
variables: `RAG_DOCUMENT_DIR`, `RAG_INDEX_PATH`, `RAG_EMBEDDING_MODEL`,
`RAG_TOP_K`, and `RAG_MIN_SCORE`.

The default model is `text-embedding-3-small`. Building and querying an index
use the service's `OPENAI_API_KEY` and incur embedding API usage.
