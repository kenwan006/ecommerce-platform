"""User-scoped conversational and durable agent memory.

Conversation messages are the audit/history record.  pgvector is used only
for curated, durable memories; live order and payment data remains in Spring
services and is never treated as memory.
"""

from __future__ import annotations

import json
import logging
import re
from dataclasses import dataclass
from typing import Any
from uuid import UUID, uuid4

from openai import AsyncOpenAI
from psycopg import AsyncConnection

from app.rag import to_vector_literal

logger = logging.getLogger(__name__)

ALLOWED_MEMORY_TYPES = {
    "communication_preference",
    "shopping_preference",
    "accessibility_preference",
    "long_running_goal",
    "confirmed_decision",
    "explicit_requested_memory",
}

# Defense in depth beyond the extraction prompt. The list deliberately errs on
# the side of rejecting sensitive data; live backend tools provide transactional
# data when it is needed.
SENSITIVE_MEMORY_PATTERN = re.compile(
    r"\b(card number|credit card|cvv|cvc|password|access token|bearer token|"
    r"social security|ssn|bank account|routing number|full address)\b",
    re.IGNORECASE,
)
TRANSIENT_MEMORY_PATTERN = re.compile(
    r"\b(order|payment|refund|shipment|inventory|return)\s*(#?\d+|status|is|was|has)\b",
    re.IGNORECASE,
)


@dataclass(frozen=True)
class ConversationContext:
    conversation_id: str
    summary: str
    recent_messages: list[dict[str, str]]
    durable_memories: list[str]


@dataclass(frozen=True)
class ExtractedMemory:
    memory_type: str
    content: str
    confidence: float


def validated_memories(payload: Any) -> list[ExtractedMemory]:
    """Validate untrusted LLM extraction output before it can be persisted."""
    if not isinstance(payload, dict) or not isinstance(payload.get("memories"), list):
        return []
    accepted: list[ExtractedMemory] = []
    for item in payload["memories"]:
        if not isinstance(item, dict):
            continue
        memory_type = item.get("type")
        content = item.get("content")
        confidence = item.get("confidence")
        if (
            memory_type not in ALLOWED_MEMORY_TYPES
            or not isinstance(content, str)
            or not isinstance(confidence, (float, int))
        ):
            continue
        content = " ".join(content.split())
        if not content or len(content) > 500 or confidence < 0.85:
            continue
        if SENSITIVE_MEMORY_PATTERN.search(content) or TRANSIENT_MEMORY_PATTERN.search(content):
            continue
        accepted.append(ExtractedMemory(memory_type, content, min(float(confidence), 1.0)))
    return accepted


class MemoryService:
    """Owns memory persistence, retrieval, summaries, and safe extraction."""

    def __init__(
        self,
        *,
        database_url: str,
        embedding_model: str,
        model: str,
        tenant_id: str,
        history_limit: int,
        summary_threshold: int,
        memory_top_k: int,
        memory_min_score: float,
        client: AsyncOpenAI | None = None,
    ):
        self.database_url = database_url
        self.embedding_model = embedding_model
        self.model = model
        self.tenant_id = tenant_id
        self.history_limit = history_limit
        self.summary_threshold = summary_threshold
        self.memory_top_k = memory_top_k
        self.memory_min_score = memory_min_score
        self.client = client or AsyncOpenAI()

    async def prepare_context(
        self, *, user_id: str, conversation_id: str | None, question: str
    ) -> ConversationContext:
        conversation_id = await self._get_or_create_conversation(user_id, conversation_id)
        summary, recent_messages = await self._conversation_context(user_id, conversation_id)
        durable_memories = await self._retrieve_memories(user_id, question)
        return ConversationContext(conversation_id, summary, recent_messages, durable_memories)

    async def save_message(
        self, *, user_id: str, conversation_id: str, role: str, content: str
    ) -> str:
        message_id = str(uuid4())
        async with await AsyncConnection.connect(self.database_url) as connection:
            async with connection.transaction():
                async with connection.cursor() as cursor:
                    # Ownership is checked again at write time, not only at read time.
                    await cursor.execute(
                        """
                        INSERT INTO conversation_messages
                            (id, conversation_id, tenant_id, user_id, role, content)
                        SELECT %s, id, tenant_id, user_id, %s, %s
                        FROM conversations
                        WHERE id = %s AND tenant_id = %s AND user_id = %s AND archived_at IS NULL
                        """,
                        (message_id, role, content, conversation_id, self.tenant_id, user_id),
                    )
                    if cursor.rowcount != 1:
                        raise PermissionError("Conversation was not found for the authenticated user.")
                    await cursor.execute(
                        "UPDATE conversations SET updated_at = now() WHERE id = %s",
                        (conversation_id,),
                    )
        return message_id

    async def process_completed_turn(
        self,
        *,
        user_id: str,
        conversation_id: str,
        user_message_id: str,
        assistant_message_id: str,
    ) -> None:
        """Best-effort post-reply work; a production system uses a durable worker."""
        try:
            await self._extract_and_store_memories(
                user_id, conversation_id, user_message_id, assistant_message_id
            )
            await self._maybe_refresh_summary(user_id, conversation_id)
        except Exception:
            logger.exception("Post-reply memory processing failed for conversation %s", conversation_id)

    async def _get_or_create_conversation(self, user_id: str, requested_id: str | None) -> str:
        if requested_id:
            try:
                UUID(requested_id)
            except ValueError as exception:
                raise ValueError("conversationId must be a UUID.") from exception
            async with await AsyncConnection.connect(self.database_url) as connection:
                async with connection.cursor() as cursor:
                    await cursor.execute(
                        """
                        SELECT id FROM conversations
                        WHERE id = %s AND tenant_id = %s AND user_id = %s AND archived_at IS NULL
                        """,
                        (requested_id, self.tenant_id, user_id),
                    )
                    if await cursor.fetchone():
                        return requested_id
            raise PermissionError("Conversation was not found for the authenticated user.")

        conversation_id = str(uuid4())
        async with await AsyncConnection.connect(self.database_url) as connection:
            async with connection.cursor() as cursor:
                await cursor.execute(
                    """
                    INSERT INTO conversations (id, tenant_id, user_id)
                    VALUES (%s, %s, %s)
                    """,
                    (conversation_id, self.tenant_id, user_id),
                )
        return conversation_id

    async def _conversation_context(self, user_id: str, conversation_id: str) -> tuple[str, list[dict[str, str]]]:
        async with await AsyncConnection.connect(self.database_url) as connection:
            async with connection.cursor() as cursor:
                await cursor.execute(
                    """
                    SELECT summary FROM conversations
                    WHERE id = %s AND tenant_id = %s AND user_id = %s AND archived_at IS NULL
                    """,
                    (conversation_id, self.tenant_id, user_id),
                )
                row = await cursor.fetchone()
                if not row:
                    raise PermissionError("Conversation was not found for the authenticated user.")
                await cursor.execute(
                    """
                    SELECT role, content FROM conversation_messages
                    WHERE conversation_id = %s AND tenant_id = %s AND user_id = %s
                    ORDER BY created_at DESC
                    LIMIT %s
                    """,
                    (conversation_id, self.tenant_id, user_id, self.history_limit),
                )
                rows = list(reversed(await cursor.fetchall()))
        return row[0], [{"role": role, "content": content} for role, content in rows]

    async def _retrieve_memories(self, user_id: str, question: str) -> list[str]:
        embedding = await self.client.embeddings.create(model=self.embedding_model, input=question)
        vector = to_vector_literal(embedding.data[0].embedding)
        async with await AsyncConnection.connect(self.database_url) as connection:
            async with connection.cursor() as cursor:
                await cursor.execute(
                    """
                    SELECT memory_text, 1 - (embedding <=> %s::vector) AS score
                    FROM user_memories
                    WHERE tenant_id = %s
                      AND user_id = %s
                      AND status = 'active'
                      AND (expires_at IS NULL OR expires_at > now())
                    ORDER BY embedding <=> %s::vector
                    LIMIT %s
                    """,
                    (vector, self.tenant_id, user_id, vector, self.memory_top_k),
                )
                return [
                    memory_text
                    for memory_text, score in await cursor.fetchall()
                    if float(score) >= self.memory_min_score
                ]

    async def _extract_and_store_memories(
        self, user_id: str, conversation_id: str, user_message_id: str, assistant_message_id: str
    ) -> None:
        messages = await self._exchange(user_id, conversation_id, user_message_id, assistant_message_id)
        if not messages:
            return
        completion = await self.client.chat.completions.create(
            model=self.model,
            response_format={"type": "json_object"},
            messages=[
                {"role": "system", "content": self._memory_extraction_prompt()},
                {"role": "user", "content": json.dumps(messages)},
            ],
        )
        raw = completion.choices[0].message.content or '{"memories": []}'
        memories = validated_memories(json.loads(raw))
        for memory in memories:
            await self._upsert_memory(user_id, conversation_id, user_message_id, memory)

    async def _exchange(
        self, user_id: str, conversation_id: str, user_message_id: str, assistant_message_id: str
    ) -> list[dict[str, str]]:
        async with await AsyncConnection.connect(self.database_url) as connection:
            async with connection.cursor() as cursor:
                await cursor.execute(
                    """
                    SELECT role, content FROM conversation_messages
                    WHERE conversation_id = %s AND tenant_id = %s AND user_id = %s
                      AND id IN (%s, %s)
                    ORDER BY CASE id
                        WHEN %s::uuid THEN 0
                        WHEN %s::uuid THEN 1
                    END
                    """,
                    (
                        conversation_id,
                        self.tenant_id,
                        user_id,
                        user_message_id,
                        assistant_message_id,
                        user_message_id,
                        assistant_message_id,
                    ),
                )
                rows = await cursor.fetchall()
        return [{"role": role, "content": content} for role, content in rows]

    async def _upsert_memory(
        self, user_id: str, conversation_id: str, source_message_id: str, memory: ExtractedMemory
    ) -> None:
        embedding = await self.client.embeddings.create(model=self.embedding_model, input=memory.content)
        vector = to_vector_literal(embedding.data[0].embedding)
        async with await AsyncConnection.connect(self.database_url) as connection:
            async with connection.transaction():
                async with connection.cursor() as cursor:
                    # One current value per type keeps this MVP explainable. A production
                    # version uses semantic near-duplicate detection plus memory versions.
                    await cursor.execute(
                        """
                        UPDATE user_memories
                        SET status = 'superseded', updated_at = now()
                        WHERE tenant_id = %s AND user_id = %s AND memory_type = %s AND status = 'active'
                        """,
                        (self.tenant_id, user_id, memory.memory_type),
                    )
                    await cursor.execute(
                        """
                        INSERT INTO user_memories
                            (tenant_id, user_id, memory_type, memory_text, embedding,
                             source_conversation_id, source_message_id, confidence)
                        VALUES (%s, %s, %s, %s, %s::vector, %s, %s, %s)
                        """,
                        (
                            self.tenant_id,
                            user_id,
                            memory.memory_type,
                            memory.content,
                            vector,
                            conversation_id,
                            source_message_id,
                            memory.confidence,
                        ),
                    )

    async def _maybe_refresh_summary(self, user_id: str, conversation_id: str) -> None:
        async with await AsyncConnection.connect(self.database_url) as connection:
            async with connection.cursor() as cursor:
                await cursor.execute(
                    """
                    SELECT summary, summary_through_message_id
                    FROM conversations
                    WHERE id = %s AND tenant_id = %s AND user_id = %s
                    """,
                    (conversation_id, self.tenant_id, user_id),
                )
                row = await cursor.fetchone()
                if not row:
                    return
                summary, through_message_id = row
                if through_message_id:
                    await cursor.execute(
                        """
                        SELECT id, role, content FROM conversation_messages
                        WHERE conversation_id = %s AND tenant_id = %s AND user_id = %s
                          AND created_at > (
                              SELECT created_at FROM conversation_messages WHERE id = %s
                          )
                        ORDER BY created_at ASC
                        """,
                        (conversation_id, self.tenant_id, user_id, through_message_id),
                    )
                else:
                    await cursor.execute(
                        """
                        SELECT id, role, content FROM conversation_messages
                        WHERE conversation_id = %s AND tenant_id = %s AND user_id = %s
                        ORDER BY created_at ASC
                        """,
                        (conversation_id, self.tenant_id, user_id),
                    )
                messages = await cursor.fetchall()
        if len(messages) < self.summary_threshold:
            return
        response = await self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": self._summary_prompt()},
                {"role": "user", "content": json.dumps({"existing_summary": summary, "messages": messages}, default=str)},
            ],
        )
        updated_summary = (response.choices[0].message.content or "").strip()
        if not updated_summary:
            return
        final_message_id = messages[-1][0]
        async with await AsyncConnection.connect(self.database_url) as connection:
            async with connection.cursor() as cursor:
                await cursor.execute(
                    """
                    UPDATE conversations
                    SET summary = %s, summary_through_message_id = %s, updated_at = now()
                    WHERE id = %s AND tenant_id = %s AND user_id = %s
                      AND summary_through_message_id IS NOT DISTINCT FROM %s
                    """,
                    (
                        updated_summary,
                        final_message_id,
                        conversation_id,
                        self.tenant_id,
                        user_id,
                        through_message_id,
                    ),
                )

    @staticmethod
    def _memory_extraction_prompt() -> str:
        return """Extract only durable, user-specific memory from this most recent chat exchange.
Return JSON exactly as {\"memories\": [{\"type\": string, \"content\": string, \"confidence\": number}]}.
Allowed types: communication_preference, shopping_preference, accessibility_preference,
long_running_goal, confirmed_decision, explicit_requested_memory.
Save only explicit stable preferences, explicit requests to remember something, long-running
goals, or confirmed decisions useful in future conversations. Do not infer facts.
Never save current order/payment/refund/shipment/inventory status; those are available from
live APIs. Never save payment details, credentials, tokens, full addresses, or sensitive
personal/financial information. Temporary questions and casual statements are not memories.
Return an empty list if no item qualifies."""

    @staticmethod
    def _summary_prompt() -> str:
        return """Produce a concise rolling conversation summary. Preserve the customer's goal,
unresolved questions, confirmed decisions, and relevant order IDs only as references.
Do not include payment credentials, addresses, access tokens, or claims that may be stale.
Order/payment/shipment status must be retrieved live, not treated as summary truth."""
