from agents import RunContextWrapper, function_tool

from app.agents.context import AgentRequestContext



@function_tool
async def get_order_status(
    context: RunContextWrapper[AgentRequestContext],
    order_id: int,
) -> dict:
    """Get the status and details of one order belonging to the signed-in customer.

    Args:
        order_id: The order ID supplied by the customer.
    """
    return await context.context.spring_client.get_order(
        order_id,
        context.context.authorization,
    )


@function_tool
async def get_pending_orders(
    context: RunContextWrapper[AgentRequestContext],
) -> list:
    """Get all signed-in customer orders whose payment is still pending."""
    return await context.context.spring_client.get_pending_payment_orders(
        context.context.authorization,
    )


@function_tool
async def get_recent_orders(
    context: RunContextWrapper[AgentRequestContext],
) -> list:
    """Get all recent orders for the signed-in customer, including paid and pending orders."""
    return await context.context.spring_client.get_orders(
        context.context.authorization,
    )
