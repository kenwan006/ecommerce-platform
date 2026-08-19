from agents import Agent, Runner
from agents.mcp import MCPServerStreamableHttp

from app.agents.context import AgentRequestContext
from app.clients.spring_client import SpringClient
from app.tools.order_tools import get_order_status, get_pending_orders


class OrderAgent:
    def __init__(self, mcp_servers: list[MCPServerStreamableHttp]):
        self.agent = Agent(
            name="E-commerce Assistant",
            model="gpt-4o-mini",
            instructions=(
                "You are the ecommerce order assistant. Help customers with their orders. "
                "Use get_order_status when the customer provides an order ID. "
                "Use get_pending_orders when the customer asks generally about their order "
                "status or pending orders without providing an order ID. "
                "Use the OpenAI documentation MCP tools for questions about OpenAI APIs, "
                "models, or SDKs. Explain both the order status and payment status. "
                "Use the GitHub MCP tools for questions about repositories, issues, pull "
                "requests, and source code. For GitHub questions, call a GitHub tool rather "
                "than answering from general knowledge. When a customer asks about 'my' "
                "repositories, first identify the authenticated GitHub user, then search for "
                "that user's repositories. Only say GitHub access is unavailable if a GitHub "
                "tool returns an authentication or authorization error. "
                "Do not invent order data."
            ),
            tools=[get_order_status, get_pending_orders],
            mcp_servers=mcp_servers,
        )

    async def answer(
        self,
        question: str,
        authorization: str,
        spring_client: SpringClient,
    ) -> str:
        result = await Runner.run(
            self.agent,
            question,
            context=AgentRequestContext(
                authorization=authorization,
                spring_client=spring_client,
            ),
        )
        return str(result.final_output)
