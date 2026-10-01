from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from agents.mcp import MCPServerStreamableHttp

from app.agents.order_agent import OrderAgent
from app.api.ai_routes import router as ai_router
from app.clients.spring_client import SpringClient
from app.config import settings
from app.rag import PostgresRag


@asynccontextmanager
async def lifespan(app: FastAPI):
    if not settings.openai_api_key:
        raise RuntimeError("OPENAI_API_KEY must be set before starting the AI agent.")
    if not settings.github_token:
        raise RuntimeError("GITHUB_TOKEN must be set before starting the AI agent.")

    openai_docs_mcp_server = MCPServerStreamableHttp(
        name="OpenAI Docs",
        params={"url": settings.openai_docs_mcp_url},
        cache_tools_list=True,
    )
    github_mcp_server = MCPServerStreamableHttp(
        name="GitHub",
        params={
            "url": settings.github_mcp_url,
            "headers": {
                "Authorization": f"Bearer {settings.github_token}",
                "X-MCP-Toolsets": "context,repos,issues,pull_requests,users",
                "X-MCP-Readonly": "true",
            },
        },
        cache_tools_list=True,
    )

    try:
        await openai_docs_mcp_server.connect()
        await openai_docs_mcp_server.list_tools()
        await github_mcp_server.connect()
        await github_mcp_server.list_tools()

        app.state.spring_client = SpringClient(settings.spring_api_url)
        app.state.order_agent = OrderAgent(
            [openai_docs_mcp_server, github_mcp_server],
            PostgresRag(
                database_url=settings.rag_database_url,
                embedding_model=settings.rag_embedding_model,
                top_k=settings.rag_top_k,
                min_score=settings.rag_min_score,
            ),
        )
        yield
    finally:
        await github_mcp_server.cleanup()
        await openai_docs_mcp_server.cleanup()


app = FastAPI(title="Ecommerce AI Agent", lifespan=lifespan)
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://localhost:5174",
        "http://127.0.0.1:5173",
        "http://127.0.0.1:5174",
    ],
    allow_credentials=True,
    allow_methods=["POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)
app.include_router(ai_router)
