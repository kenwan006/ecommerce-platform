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
  -> Commerce calls Payment service for an idempotent Stripe refund
  -> Payment marks the original Payment record REFUNDED and creates payment_refunds row
  -> Commerce marks the Return, payment status, and Order status REFUNDED
```

Key records:

- Commerce: `return_requests`, `return_items`, order status history.
- Warehouse: `return_receipts`, inventory movement ledger.
- Payment: original payment plus a separate `payment_refunds` money-movement record.

The demo's label is a fake label URL and tracking number. A production implementation would have a carrier/shipping service create the prepaid label and protect the Warehouse-to-Commerce inspection endpoint with service-to-service authentication. It would also make refund completion webhook-driven, rather than treating Stripe's synchronous refund response as final.
