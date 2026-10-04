# Order and Payment Business Flow

This guide describes the core order and payment lifecycle in the current single-merchant platform. The same foundations apply to a large marketplace such as Temu, although a marketplace adds seller orders, split fulfillment, settlement, and cross-border concerns.

## Scope and ownership

```text
Customer
  → Commerce: cart, checkout, customer-facing order state
  → Warehouse: inventory reservation and fulfillment work
  → Payment: provider payment record and Stripe webhook processing
  → Stripe: payment authorization/confirmation and provider risk controls
```

Each service owns its own data. Commerce never directly edits Warehouse or Payment tables.

| Service | Owns | Does not own |
|---|---|---|
| Commerce | cart, order, order items, checkout idempotency, customer order state | physical inventory, provider payment state |
| Warehouse | available/reserved stock, reservation, fulfillment, shipment | customer checkout or Stripe calls |
| Payment | payment record, Stripe PaymentIntent ID, webhook result, payment outbox | order state or inventory |

## 1. Cart

The customer adds products to a persistent cart. A cart is a convenience and intent-to-purchase record; it is not a promise that inventory will still be available.

```text
POST /api/cart/items
  → Commerce writes carts and cart_items
  → No Warehouse call
  → No inventory reservation
```

Why no reservation? Reserving every cart item lets abandoned carts exhaust stock. A customer can keep a cart for days; a reservation should be short-lived and tied to an active checkout.

## 2. Checkout starts

When the customer clicks **Continue to secure payment**, React generates a `checkoutId` and sends the cart items to Commerce.

```text
POST /api/checkout
{
  "userId": 1,
  "checkoutId": "client-generated-uuid",
  "items": [{"productId": 3, "quantity": 2}]
}
```

`checkoutId` is the business idempotency key. Retrying the same request must recover the same order instead of creating a second order and a second payment attempt.

Commerce then performs this synchronous preparation flow:

```text
1. Find or create the local order using (user_id, checkout_id).
2. Ask Warehouse to reserve inventory.
3. Run the platform fraud decision.
4. Ask Payment to create or reuse a Stripe PaymentIntent.
5. Return paymentIntentId and clientSecret to React.
```

The reservation is created with an expiry. It moves stock from `available` to `reserved` only if sufficient stock exists. The stock operation must be atomic so two customers cannot buy the final unit.

## 3. Payment confirmation

React uses Stripe Elements and the PaymentIntent `clientSecret` to collect payment details. Card details go directly from the browser to Stripe; our services do not receive or store card numbers.

```text
Browser → Stripe Elements → Stripe
                         ↓
                 payment_intent.succeeded / payment_intent.payment_failed
```

The immediate browser result is useful for the UI, but it is not the source of truth. Stripe’s signed webhook is the source of truth because the browser may close, lose its network connection, or be tampered with.

## 4. Webhook to durable payment event

```text
Stripe webhook
  → Payment service validates Stripe signature
  → updates local payment row
  → writes an outbox event in the same database transaction
  → scheduled outbox publisher sends event to Kafka
```

The event is either `PaymentSucceeded` or `PaymentFailed`.

The outbox is important: without it, the database update could succeed while the Kafka publish fails, leaving other services unaware of the result. With an outbox, the publisher can retry until the event is sent.

## 5. Order, inventory, and fulfillment react independently

Both Commerce and Warehouse consume `payment-events` with their own Kafka consumer groups.

```text
PaymentSucceeded
  ├── Commerce: PENDING_PAYMENT → PAID
  └── Warehouse: RESERVED → CONFIRMED; create READY_TO_PICK fulfillment

PaymentFailed
  ├── Commerce: PENDING_PAYMENT → PAYMENT_FAILED
  └── Warehouse: RESERVED → RELEASED; make stock available again
```

Consumers record processed event IDs. Kafka is at-least-once delivery, so an event may arrive more than once. The inbox/processed-event record makes handling idempotent.

## 6. Order state machine

Commerce owns the customer-facing order state. State changes are driven by named business events rather than arbitrary strings.

```text
PENDING_PAYMENT
  ├── PAYMENT_SUCCEEDED      → PAID
  ├── PAYMENT_FAILED         → PAYMENT_FAILED
  ├── FRAUD_REVIEW_REQUIRED  → FRAUD_REVIEW
  ├── FRAUD_DECLINED         → FRAUD_DECLINED
  └── CANCELLED              → CANCELLED

FRAUD_REVIEW
  ├── REVIEW_APPROVED        → PENDING_PAYMENT
  ├── REVIEW_DECLINED        → FRAUD_DECLINED
  └── CANCELLED              → CANCELLED

PAID
  └── REFUND_SUCCEEDED       → REFUNDED
```

A transition has:

```text
source state + event + guard → target state + action
```

Example:

```text
PENDING_PAYMENT + PAYMENT_SUCCEEDED
  + guard: incoming PaymentIntent ID matches the order
  → PAID
  + action: write order_status_history audit record
```

## 7. Failure and recovery cases

| Situation | Expected behavior |
|---|---|
| Customer retries checkout after timeout | Same `checkoutId` returns the same order and provider payment intent. |
| Inventory is unavailable | Warehouse rejects the reservation; no payment should begin. |
| Customer closes the browser | Reservation expires and a scheduled job releases it. Stripe webhook can still finalize payment if payment completed. |
| Stripe webhook is delivered twice | Payment uses the provider event ID/idempotency checks; downstream consumers use processed-event IDs. |
| Payment fails | Payment publishes `PaymentFailed`; Warehouse releases the reservation. |
| Payment succeeds but UI never sees it | Webhook and Kafka still drive the final state. Customer sees the paid order after refresh. |
| Fraud declines after reservation | Commerce asks Warehouse to release inventory; no payment intent is created. |

## 8. Unhappy paths and concurrency

### Two customers try to buy the final item

Assume SKU `TOTE-001` has one unit available.

```text
Customer A checkout ─┐
                     ├─ Warehouse atomic reservation → succeeds
Customer B checkout ─┘
                     └─ Warehouse atomic reservation → fails: OUT_OF_STOCK
```

The important rule is that the availability check and reservation must be one atomic database operation, not two separate reads/writes:

```text
Incorrect:
  read available = 1
  if available > 0, reserve it

Correct:
  atomically reserve only when available quantity is sufficient
```

Typical relational-database implementations use either:

- a conditional update such as `UPDATE ... WHERE on_hand - reserved >= requested_quantity`; or
- a transaction with row locking/optimistic versioning on the inventory row.

Only the customer whose reservation succeeds proceeds to payment. The losing customer receives a clear out-of-stock response; Commerce must not create a PaymentIntent for unavailable items.

### Payment provider timeout

Commerce may not know whether Stripe received the first request. Retrying with the same `checkoutId` and the same provider idempotency key lets Stripe return the original PaymentIntent instead of charging twice.

```text
Commerce → Stripe create PaymentIntent → network timeout
Commerce retries with same idempotency key
  → Stripe returns existing PaymentIntent
```

### Customer pays as reservation expires

This is a real race. The reservation-expiry job and `PaymentSucceeded` consumer may arrive at nearly the same time.

The Warehouse reservation state machine must make the result deterministic and idempotent:

```text
RESERVED → CONFIRMED
RESERVED → EXPIRED
```

Whichever valid transition commits first wins. A mature policy must define the compensation when payment succeeds after the reservation has expired:

1. Re-reserve stock if it is still available, then fulfill; or
2. Create a manual exception / backorder; or
3. Automatically refund the payment.

The checkout reservation TTL should be long enough for normal payment and 3-D Secure flows, reducing this race.

### Payment succeeds but downstream processing fails

```text
Stripe confirms payment
  → Payment database transaction commits
  → Kafka temporarily unavailable
  → outbox row remains unpublished
  → publisher retries until event is delivered
```

If Commerce or Warehouse cannot process the delivered event after retries, send it to a dead-letter topic, alert operators, and retain the original event/correlation ID for replay.

### Invalid or duplicate provider webhooks

- Invalid signature: reject without changing the payment record.
- Duplicate webhook: recognize the provider event ID and make no second state change.
- Events out of order: only allow the transition permitted by the payment state machine; never regress `PAID` back to `PENDING`.

### Payment requires additional customer action

Some payments are neither immediate success nor immediate failure: card authentication, redirected payment methods, and asynchronous bank methods can remain `REQUIRES_ACTION` or `PROCESSING`.

The order should stay `PENDING_PAYMENT` while the payment is pending. Final state must still come from the provider webhook, not only from the browser redirect.

### Cart price or promotion changed

Never trust the client-provided total. At checkout, Commerce reloads the products and recalculates price, discount, shipping, tax, and final total server-side. If the displayed price changed, return the updated checkout summary and require customer confirmation where the business requires it.

## 9. Do we need Redis for real-time stock?

Not as the sole source of truth.

The reliable stock equation is:

```text
available_to_sell = on_hand_quantity - reserved_quantity
```

That durable inventory state belongs in the Warehouse database or a dedicated inventory ledger. It must survive Redis eviction, failover, and cache inconsistency.

Redis is still useful at scale:

| Redis use | Why it helps | Source of truth? |
|---|---|---|
| product availability cache | reduces database reads for product pages | No |
| cart/session cache | low-latency temporary data | No, unless deliberately designed as durable cart storage |
| rate limiting | protects checkout/inventory APIs | No |
| short-lived checkout hold cache | can reduce contention for very hot SKUs | No; reconcile against durable inventory |
| distributed locks | sometimes used carefully for special workflows | Usually prefer atomic database reservation first |

For moderate traffic, MySQL/Postgres with correct transactions, row versioning, and indexes is the right initial source of truth. For high-QPS hot products, an inventory service may use partitioned inventory, atomic Redis/Lua counters, reservation queues, or database sharding—but it still needs durable reconciliation and a ledger. Redis makes a high-scale design faster; it does not remove the need for correct reservation semantics.

## Important invariants

These are good statements to use in an interview:

1. An order is created at most once per customer checkout ID.
2. A payment provider request is idempotent for that order/checkout.
3. Available inventory never becomes negative.
4. A reservation can be confirmed, released, or expired exactly once from a business perspective; repeated commands are harmless.
5. A payment webhook is verified before it changes payment state.
6. Every accepted order transition is auditable.
7. The customer-facing order state and provider payment state are related but not identical; each service remains the source of truth for its own state.

## 10. Current implementation versus production maturity

The demo covers the core flow, but a mature commerce platform also needs the following.

### High-priority gaps

- **Order cancellation policy:** define cancellation windows and prevent cancellation after a fulfillment/shipment boundary.
- **Payment authorization versus capture:** many businesses authorize at checkout but capture only when items ship; the current demo treats Stripe success as paid immediately.
- **Partial fulfillment and partial refund:** support an order with multiple shipments or only some items refunded.
- **Payment reconciliation:** daily job compares our payment ledger with provider settlement reports and investigates mismatches.
- **Dead-letter handling:** failed Kafka events need retry policy, observability, and a dead-letter topic.
- **Service-to-service authentication:** internal REST calls currently need production authentication/authorization.
- **Inventory concurrency:** use optimistic locking or atomic SQL updates, and test high-concurrency “last item” scenarios.

### Marketplace-specific gaps

A marketplace introduces a parent checkout order and child seller orders:

```text
Customer order #100
  ├── Seller A order #100-1 → fulfillment/shipment A
  └── Seller B order #100-2 → fulfillment/shipment B
```

It also requires seller catalog ownership, commissions, payout/settlement, seller performance, disputes, promotion allocation, tax, duty, and cross-border carrier flows.

## Interview prompts to practice

1. Why reserve inventory at checkout rather than add-to-cart?
2. How do you avoid duplicate orders if the client retries a timed-out request?
3. Why is a Stripe webhook more reliable than trusting the browser callback?
4. Why use an outbox when publishing payment events?
5. How do you make Kafka consumers idempotent?
6. What happens if payment succeeds but Warehouse is temporarily down?
7. When would you authorize payment versus capture payment?
8. How would you split one customer order across sellers or warehouses?
9. How do refunds, chargebacks, and settlement differ?
10. What monitoring would detect a stuck payment, reservation, or fulfillment?
