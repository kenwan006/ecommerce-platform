import logging
from typing import Annotated, Optional

from fastapi import APIRouter, Depends, Header, HTTPException, Request

from app.agents.order_agent import OrderAgent
from app.clients.spring_client import SpringClient
from app.models import AskRequest, AskResponse

router = APIRouter(prefix="/api/ai", tags=["AI"])
logger = logging.getLogger(__name__)


def get_order_agent(request: Request) -> OrderAgent:
    return request.app.state.order_agent


def get_spring_client(request: Request) -> SpringClient:
    return request.app.state.spring_client


@router.post("/ask", response_model=AskResponse)
async def ask(
    ask_request: AskRequest,
    order_agent: Annotated[OrderAgent, Depends(get_order_agent)],
    spring_client: Annotated[SpringClient, Depends(get_spring_client)],
    authorization: Optional[str] = Header(default=None),
) -> AskResponse:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="A bearer token is required.")

    try:
        answer = await order_agent.answer(
            ask_request.message,
            authorization,
            spring_client,
        )
    except TimeoutError as exception:
        raise HTTPException(status_code=504, detail=str(exception)) from exception
    except Exception as exception:
        logger.exception("Agent request failed")
        raise HTTPException(
            status_code=502,
            detail="The assistant could not complete the request. Check the agent-service console.",
        ) from exception
    return AskResponse(answer=answer.answer, refund_action=answer.refund_action)
