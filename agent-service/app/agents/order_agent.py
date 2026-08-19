import json
from typing import Any, List

from app.clients.spring_client import SpringClient
from app.tools.order_tools import (
    ORDER_STATUS_TOOL,
    PENDING_ORDERS_TOOL,
    get_order_status,
    get_pending_orders,
)


class OrderAgent:
    def __init__(self, openai_client: Any, spring_client: SpringClient):
        self.openai_client = openai_client
        self.spring_client = spring_client

    def answer(self, question: str, authorization: str) -> str:
        input_items: List[Any] = [
            {
                "role": "system",
                "content": (
                    "You are the ecommerce order assistant. Help customers with their orders. "
                    "Use get_order_status when the customer provides an order ID. "
                    "Use get_pending_orders when the customer asks generally about their order status "
                    "or pending orders without providing an order ID. "
                    "Explain both the order status and payment status. "
                    "Do not invent order data."
                ),
            },
            {"role": "user", "content": question},
        ]

        response = self.openai_client.responses.create(
            model="gpt-4o-mini",
            input=input_items,
            tools=[ORDER_STATUS_TOOL, PENDING_ORDERS_TOOL],
        )

        while function_calls := [item for item in response.output if item.type == "function_call"]:
            input_items.extend(response.output)

            for function_call in function_calls:
                if function_call.name == "get_order_status":
                    arguments = json.loads(function_call.arguments)
                    tool_result = get_order_status(
                        order_id=arguments["orderId"],
                        authorization=authorization,
                        spring_client=self.spring_client,
                    )
                elif function_call.name == "get_pending_orders":
                    tool_result = get_pending_orders(
                        authorization=authorization,
                        spring_client=self.spring_client,
                    )
                else:
                    raise RuntimeError("The agent requested an unsupported tool.")
                input_items.append(
                    {
                        "type": "function_call_output",
                        "call_id": function_call.call_id,
                        "output": json.dumps(tool_result),
                    }
                )

            response = self.openai_client.responses.create(
                model="gpt-4o-mini",
                input=input_items,
                tools=[ORDER_STATUS_TOOL, PENDING_ORDERS_TOOL],
            )

        return response.output_text
