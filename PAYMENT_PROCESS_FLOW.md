# Stripe Payment Process Flow

```mermaid
sequenceDiagram
    autonumber
    actor Customer
    participant UI as React UI
    participant Commerce as Commerce :8080
    participant Warehouse as Warehouse :8082
    participant Payment as Payment :8083
    participant Stripe
    participant Kafka as Redpanda :9092

    Customer->>UI: Continue to payment
    UI->>Commerce: POST /api/checkout (checkoutId, items)
    Commerce->>Warehouse: REST: project products and reserve stock
    Warehouse-->>Commerce: Reservation created
    Commerce->>Payment: REST: POST /internal/payments
    Note over Commerce,Payment: checkoutId is the idempotency key
    Payment->>Stripe: Create PaymentIntent
    Stripe-->>Payment: PaymentIntent ID and client secret
    Payment-->>Commerce: Client secret
    Commerce-->>UI: Payment details
    UI->>Stripe: Confirm with Stripe Elements
    Stripe->>Payment: Signed webhook via Stripe CLI
    Payment->>Payment: Set payment state + save outbox row atomically
    Payment->>Kafka: Publish PaymentSucceeded or PaymentFailed
    Kafka->>Commerce: Consume event, update order state
    Kafka->>Warehouse: Consume event, confirm/release reservation
    Warehouse->>Warehouse: On success, create READY_TO_PICK fulfillment
```

## Important behavior

- Commerce calculates the amount from its own catalog; the browser never supplies the amount.
- Payment owns Stripe API keys, PaymentIntent records, webhook verification, and the outbox.
- Stripe's webhook is the source of truth for final payment status.
- The payment outbox makes the database update and the intent to publish an event atomic. Publishing is at-least-once, so both Commerce and Warehouse store processed event IDs to ignore duplicates.
- Repeating checkout with the same `(userId, checkoutId)` reuses the Commerce order and the existing Stripe PaymentIntent.

## Local webhook forwarding

```bash
stripe listen --forward-to localhost:8083/api/stripe/webhook
```

Place the signing secret printed by Stripe CLI only in Payment's ignored local configuration. The public `application.yml` reads it from `STRIPE_WEBHOOK_SECRET` in non-local environments.
