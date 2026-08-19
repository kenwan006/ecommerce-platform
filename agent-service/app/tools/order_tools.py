from app.clients.spring_client import SpringClient

ORDER_STATUS_TOOL = {
    "type": "function",
    "name": "get_order_status",
    "description": "Get the status and details of one of the signed-in customer's orders.",
    "parameters": {
        "type": "object",
        "properties": {
            "orderId": {
                "type": "integer",
                "description": "The order ID supplied by the customer.",
            }
        },
        "required": ["orderId"],
        "additionalProperties": False,
    },
    "strict": True,
}

PENDING_ORDERS_TOOL = {
    "type": "function",
    "name": "get_pending_orders",
    "description": "Get all orders for the signed-in customer whose payment is still pending.",
    "parameters": {
        "type": "object",
        "properties": {},
        "additionalProperties": False,
    },
    "strict": True,
}


def get_order_status(order_id: int, authorization: str, spring_client: SpringClient) -> dict:
    return spring_client.get_order(order_id, authorization)


def get_pending_orders(authorization: str, spring_client: SpringClient) -> list:
    return spring_client.get_pending_payment_orders(authorization)
