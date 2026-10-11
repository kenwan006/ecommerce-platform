# Return and refund flow

This demo uses an **inspect-before-refund** policy for a paid order.

```text
Customer requests return
  -> Commerce validates ownership and PAID order
  -> creates return_requests + return_items records
  -> auto-approves the return and provides a demo prepaid label/RMA

Customer sends package back
  -> Warehouse receives and inspects it
  -> SELLABLE: Warehouse increases on-hand inventory and records RETURN_RECEIPT movement
  -> DAMAGED: Warehouse records receipt but does not restock it

Warehouse completion
  -> Warehouse records the receipt/restock and writes ReturnInspected to its outbox
  -> Warehouse outbox publisher sends ReturnInspected to Kafka topic warehouse-events
  -> Commerce consumes ReturnInspected, applies refund policy, and requests an idempotent refund from Payment
  -> Payment creates a payment_refunds row with PENDING status
  -> Stripe sends a signed refund.updated webhook when the refund reaches a terminal state
  -> Payment atomically updates the refund, records the Stripe event ID, and writes an outbox event
  -> Commerce consumes RefundSucceeded or RefundFailed and updates the Return and Order
```

Key records:

- Commerce: `return_requests`, `return_items`, order status history.
- Warehouse: `return_receipts`, inventory movement ledger.
- Payment: original payment plus a separate `payment_refunds` money-movement record.

Service ownership is deliberate: Warehouse owns the physical inspection and inventory facts;
Commerce owns the return-policy decision and refund request; Payment alone owns Stripe keys, the
Stripe Refund API call, and signed webhook processing.

The demo's label is a fake label URL and tracking number. A production implementation would have a carrier/shipping service create the prepaid label and protect the Warehouse-to-Commerce inspection endpoint with service-to-service authentication.

Refund completion is webhook-driven: the synchronous Stripe Refund API response only means that
Stripe accepted the refund request. The `refund.updated` webhook is the authority for its final
`SUCCEEDED` or `FAILED` status. The Payment service stores Stripe event IDs in
`payment_webhook_events`, so retry deliveries are safe.
