from agents import Agent, Runner
from agents.mcp import MCPServerStreamableHttp

from app.agents.context import AgentRequestContext
from app.clients.spring_client import SpringClient
from app.tools.order_tools import get_order_status, get_pending_orders


class OrderAgent:
    def __init__(self, openai_docs_mcp_server: MCPServerStreamableHttp):
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
                "Do not invent order data."
            ),
            tools=[get_order_status, get_pending_orders],
            mcp_servers=[openai_docs_mcp_server],
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
