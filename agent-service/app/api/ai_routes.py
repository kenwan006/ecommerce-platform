import logging
from typing import Annotated, Optional

from fastapi import APIRouter, BackgroundTasks, Depends, Header, HTTPException, Request
from httpx import HTTPStatusError

from app.agents.order_agent import OrderAgent
from app.clients.spring_client import SpringClient
from app.models import AskRequest, AskResponse
from app.memory import MemoryService

router = APIRouter(prefix="/api/ai", tags=["AI"])
logger = logging.getLogger(__name__)


def get_order_agent(request: Request) -> OrderAgent:
    return request.app.state.order_agent


def get_spring_client(request: Request) -> SpringClient:
    return request.app.state.spring_client


def get_memory_service(request: Request) -> MemoryService:
    return request.app.state.memory_service


@router.post("/ask", response_model=AskResponse)
async def ask(
    ask_request: AskRequest,
    background_tasks: BackgroundTasks,
    order_agent: Annotated[OrderAgent, Depends(get_order_agent)],
    spring_client: Annotated[SpringClient, Depends(get_spring_client)],
    memory_service: Annotated[MemoryService, Depends(get_memory_service)],
    authorization: Optional[str] = Header(default=None),
) -> AskResponse:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="A bearer token is required.")

    try:
        current_user = await spring_client.get_current_user(authorization)
        user_id = str(current_user["userId"])
        conversation_context = await memory_service.prepare_context(
            user_id=user_id,
            conversation_id=ask_request.conversation_id,
            question=ask_request.message,
        )
        user_message_id = await memory_service.save_message(
            user_id=user_id,
            conversation_id=conversation_context.conversation_id,
            role="user",
            content=ask_request.message,
        )
        answer = await order_agent.answer(
            ask_request.message,
            authorization,
            spring_client,
            user_id,
            conversation_context,
        )
        assistant_message_id = await memory_service.save_message(
            user_id=user_id,
            conversation_id=conversation_context.conversation_id,
            role="assistant",
            content=answer.answer,
        )
        background_tasks.add_task(
            memory_service.process_completed_turn,
            user_id=user_id,
            conversation_id=conversation_context.conversation_id,
            user_message_id=user_message_id,
            assistant_message_id=assistant_message_id,
        )
    except PermissionError as exception:
        raise HTTPException(status_code=404, detail=str(exception)) from exception
    except ValueError as exception:
        raise HTTPException(status_code=400, detail=str(exception)) from exception
    except HTTPStatusError as exception:
        if exception.response.status_code in (401, 302, 303, 307, 308):
            raise HTTPException(status_code=401, detail="The bearer token is invalid or expired.") from exception
        raise HTTPException(status_code=502, detail="Commerce identity validation failed.") from exception
    except TimeoutError as exception:
        raise HTTPException(status_code=504, detail=str(exception)) from exception
    except Exception as exception:
        logger.exception("Agent request failed")
        raise HTTPException(
            status_code=502,
            detail="The assistant could not complete the request. Check the agent-service console.",
        ) from exception
    return AskResponse(
        answer=answer.answer,
        refund_action=answer.refund_action,
        conversationId=conversation_context.conversation_id,
    )
