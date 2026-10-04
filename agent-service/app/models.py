from pydantic import BaseModel, Field


class AskRequest(BaseModel):
    message: str


class RefundAction(BaseModel):
    """A customer-confirmed action proposed by the agent, never an executed refund."""

    order_id: int = Field(gt=0)


class AskResponse(BaseModel):
    answer: str
    refund_action: RefundAction | None = None
