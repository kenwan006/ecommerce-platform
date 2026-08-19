from dataclasses import dataclass

from app.clients.spring_client import SpringClient


@dataclass(frozen=True)
class AgentRequestContext:
    """Trusted request data available to local tools, but never to the model."""

    authorization: str
    spring_client: SpringClient
