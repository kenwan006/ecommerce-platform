from app.agents.order_agent import OrderAgent
from app.memory import ConversationContext, validated_memories
from app.models import AskRequest, AskResponse


def test_memory_extraction_accepts_only_durable_safe_explicit_candidates():
    memories = validated_memories(
        {
            "memories": [
                {
                    "type": "communication_preference",
                    "content": "Customer prefers concise Chinese responses.",
                    "confidence": 0.98,
                },
                {
                    "type": "confirmed_decision",
                    "content": "Customer opted in to email shipment updates.",
                    "confidence": 0.99,
                },
                {
                    "type": "shopping_preference",
                    "content": "Customer card number is 4242.",
                    "confidence": 0.99,
                },
                {
                    "type": "unsupported_type",
                    "content": "Something unsupported.",
                    "confidence": 0.99,
                },
            ]
        }
    )

    assert memories == [
        type(memories[0])(
            memory_type="communication_preference",
            content="Customer prefers concise Chinese responses.",
            confidence=0.98,
        ),
        type(memories[0])(
            memory_type="confirmed_decision",
            content="Customer opted in to email shipment updates.",
            confidence=0.99,
        ),
    ]


def test_memory_extraction_rejects_low_confidence_and_sensitive_data():
    memories = validated_memories(
        {
            "memories": [
                {
                    "type": "shopping_preference",
                    "content": "Customer likes blue bags.",
                    "confidence": 0.70,
                },
                {
                    "type": "confirmed_decision",
                    "content": "Order 48 is currently paid.",
                    "confidence": 0.99,
                },
                {
                    "type": "explicit_requested_memory",
                    "content": "Remember my full address is 1 Main Street.",
                    "confidence": 0.99,
                },
            ]
        }
    )

    assert memories == []


def test_agent_input_contains_bounded_memory_but_preserves_current_question():
    context = ConversationContext(
        conversation_id="test",
        summary="Customer is deciding whether to return an item.",
        recent_messages=[{"role": "user", "content": "What is the return window?"}],
        durable_memories=["Customer prefers concise Chinese responses."],
    )

    prompt = OrderAgent._agent_input("Can I return order 12?", context)

    assert "Customer is deciding" in prompt
    assert "What is the return window?" in prompt
    assert "prefers concise Chinese" in prompt
    assert prompt.endswith("Can I return order 12?")


def test_conversation_id_uses_the_browser_friendly_api_field_name():
    request = AskRequest.model_validate({"message": "Hello", "conversationId": "abc"})
    response = AskResponse(answer="Hi", conversationId="abc")

    assert request.conversation_id == "abc"
    assert response.model_dump(by_alias=True)["conversationId"] == "abc"
