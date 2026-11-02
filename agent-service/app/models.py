from pydantic import BaseModel, Field


class AskRequest(BaseModel):
    message: str
    conversation_id: str | None = Field(default=None, alias="conversationId")


class RefundAction(BaseModel):
    """A customer-confirmed action proposed by the agent, never an executed refund."""

    order_id: int = Field(gt=0)


class AskResponse(BaseModel):
    answer: str
    refund_action: RefundAction | None = None
    conversation_id: str = Field(alias="conversationId")
