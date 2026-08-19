import os
from dataclasses import dataclass

from dotenv import load_dotenv

load_dotenv()


@dataclass(frozen=True)
class Settings:
    openai_api_key: str
    spring_api_url: str
    openai_docs_mcp_url: str
    github_token: str
    github_mcp_url: str


settings = Settings(
    openai_api_key=os.getenv("OPENAI_API_KEY", ""),
    spring_api_url=os.getenv("SPRING_API_URL", "http://localhost:8080"),
    openai_docs_mcp_url=os.getenv("OPENAI_DOCS_MCP_URL", "https://developers.openai.com/mcp"),
    github_token=os.getenv("GITHUB_TOKEN", ""),
    github_mcp_url=os.getenv("GITHUB_MCP_URL", "https://api.githubcopilot.com/mcp/"),
)
