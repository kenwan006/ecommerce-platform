# Agent Memory Design, Validation Plan, and Acceptance Criteria

## Purpose

The FastAPI agent keeps each customer's useful context without using memory as
the source of truth for transactional ecommerce data. Commerce, Payment, and
Warehouse APIs remain authoritative for current order, payment, refund,
shipment, and inventory state.

## Architecture

```text
Verified Commerce JWT
        |
        v
Commerce GET /api/auth/me -> authenticated user ID
        |
        +--> conversations + conversation_messages (exact, ordered history)
        |         |
        |         +--> rolling conversation summary
        |
        +--> user_memories (curated durable facts + pgvector embedding)
        |
        +--> live Spring tools for transactional facts
```

The agent service never accepts a browser-provided user ID. It asks Commerce to
validate the bearer token and returns the authenticated `userId`; that value is
used in every conversation and memory query.

## Short-term memory

`conversations` owns a user-scoped rolling `summary`; `conversation_messages`
stores original user and assistant turns. Each new request loads:

1. the summary;
2. the newest `MEMORY_HISTORY_LIMIT` messages (default 12); and
3. the current customer message.

After a completed turn, when at least `MEMORY_SUMMARY_THRESHOLD` new messages
exist (default 12), the service asks the model to merge the older messages into
the summary. Original messages remain stored for audit and deletion; the
summary only controls prompt size.

## Long-term memory

`user_memories` is separate from the shared internal-document RAG index. It
contains a tenant ID, user ID, typed text, embedding, confidence, source
conversation/message IDs, state, and optional expiry. The agent embeds the
current question, retrieves only active, non-expired memories for that same
user and tenant, applies a minimum similarity score, and adds at most
`MEMORY_TOP_K` results (default 3) to the prompt.

The post-reply extractor is intentionally narrow. It may store only:

- stable user preferences;
- communication or accessibility preferences;
- long-running goals;
- explicitly requested memories; and
- confirmed decisions that apply to future conversations.

It must not store assistant assumptions, temporary questions, current
order/payment/refund/shipment state, API-available data, payment credentials,
authentication secrets, full addresses, or other sensitive data. The model
returns structured JSON, then application-side validation enforces allowed
types, a confidence threshold (0.85), a maximum size, and sensitive-term
rejection before persistence.

For local simplicity, summary/extraction runs as a FastAPI background task.
For production, publish a durable `memory-extraction-requested` event and use
a worker with retries, dead-letter handling, tracing, and operational alerts.

## API contract

`POST /api/ai/ask` accepts:

```json
{
  "message": "Where is my order?",
  "conversationId": "optional UUID"
}
```

The response includes `conversationId`. React retains it while the chat box is
open and supplies it with the next turn. A missing ID starts a new conversation.
An existing ID belonging to another user returns `404`; it never exposes that
conversation's content.

## Validation plan

### Automated

- Unit-test extraction validation: accepted preference, low confidence,
  disallowed type, and sensitive-data examples.
- Unit-test prompt construction: summary, recent messages, and scoped durable
  memories are present; the current question remains explicit.
- Add integration tests with a temporary PostgreSQL database for conversation
  ownership, message ordering, summary watermarking, and tenant/user-scoped
  vector retrieval.
- Add API tests with a mocked Commerce `/api/auth/me` endpoint proving that an
  invalid token is rejected and another user's conversation cannot be used.
- Extend the evaluation fixture with save/do-not-save/retrieve/do-not-retrieve
  cases before changing extraction prompts or thresholds.

### Manual local verification

1. Apply `agent-service/db/001_rag.sql`, then `agent-service/db/002_memory.sql`
   to the RAG PostgreSQL database.
2. Start Commerce, FastAPI, and React; sign in as customer A.
3. Send: “Remember that I prefer concise Chinese replies.” Confirm an active
   `communication_preference` row exists for customer A.
4. Ask another question in the same chat; confirm the same `conversationId`
   is returned and two ordered message rows are added per successful turn.
5. Start a new chat / use customer B; confirm customer B cannot retrieve A's
   conversation or durable memory.
6. Ask about an order after its status changes; confirm the agent calls the
   live order tool rather than relying on an old summary or memory.

## Acceptance criteria

- [ ] The service derives user identity from the verified Commerce JWT, never
  from a frontend user ID.
- [ ] A user can continue an owned conversation and receives only their own
  summary, recent messages, and durable memories.
- [ ] Original message history is persisted with role, time, user, and
  conversation ownership.
- [ ] A summary is created only after the configured threshold and records a
  watermark so already summarized messages are not repeatedly processed.
- [ ] Long-term memory is limited to allowed durable categories and rejects
  low-confidence or sensitive candidates before writing to pgvector.
- [ ] Vector retrieval is always filtered by both tenant and user ID, status,
  expiry, and similarity threshold.
- [ ] Live transactional tools remain the source of truth for ecommerce state.
- [ ] An unauthorized or cross-user conversation request returns no data.
- [ ] Unit tests pass and the manual verification steps succeed before release.
