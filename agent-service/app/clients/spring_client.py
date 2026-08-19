import httpx


class SpringClient:
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")

    async def get_order(self, order_id: int, authorization: str) -> dict:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{self.base_url}/api/orders/{order_id}",
                headers={"Authorization": authorization},
            )

        if response.status_code == 404:
            return {
                "found": False,
                "message": "No order was found for this customer and order ID.",
            }

        response.raise_for_status()
        return response.json()

    async def get_pending_payment_orders(self, authorization: str) -> list:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(
                f"{self.base_url}/api/orders/pending-payments",
                headers={"Authorization": authorization},
            )
        response.raise_for_status()
        return response.json()
