from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from openai import OpenAI

from app.agents.order_agent import OrderAgent
from app.api.ai_routes import router as ai_router
from app.clients.spring_client import SpringClient
from app.config import settings


@asynccontextmanager
async def lifespan(app: FastAPI):
    if not settings.openai_api_key:
        raise RuntimeError("OPENAI_API_KEY must be set before starting the AI agent.")

    app.state.order_agent = OrderAgent(
        openai_client=OpenAI(api_key=settings.openai_api_key),
        spring_client=SpringClient(settings.spring_api_url),
    )
    yield


app = FastAPI(title="Ecommerce AI Agent", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:5174"],
    allow_credentials=True,
    allow_methods=["POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)
app.include_router(ai_router)
