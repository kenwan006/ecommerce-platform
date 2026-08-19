import httpx


class SpringClient:
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")

    def get_order(self, order_id: int, authorization: str) -> dict:
        response = httpx.get(
            f"{self.base_url}/api/orders/{order_id}",
            headers={"Authorization": authorization},
            timeout=10.0,
        )

        if response.status_code == 404:
            return {
                "found": False,
                "message": "No order was found for this customer and order ID.",
            }

        response.raise_for_status()
        return response.json()

    def get_pending_payment_orders(self, authorization: str) -> list:
        response = httpx.get(
            f"{self.base_url}/api/orders/pending-payments",
            headers={"Authorization": authorization},
            timeout=10.0,
        )
        response.raise_for_status()
        return response.json()
