from typing import Optional

from fastapi import APIRouter, Header, HTTPException, Request

from app.models import AskRequest, AskResponse

router = APIRouter(prefix="/api/ai", tags=["AI"])


@router.post("/ask", response_model=AskResponse)
async def ask(
    ask_request: AskRequest,
    request: Request,
    authorization: Optional[str] = Header(default=None),
) -> AskResponse:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="A bearer token is required.")

    answer = await request.app.state.order_agent.answer(
        ask_request.message,
        authorization,
        request.app.state.spring_client,
    )
    return AskResponse(answer=answer)
