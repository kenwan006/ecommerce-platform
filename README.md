# Sunridge E-commerce Platform

An interview-focused e-commerce demo that is being migrated from a modular monolith into three coarse-grained Spring Boot services. The React storefront, FastAPI agent, Stripe test integration, MySQL, and Kafka-compatible Redpanda run locally.

## Current architecture

```text
React UI (5173) ───────► Commerce (8080, sunridge_commerce)
     │                         │                 │
     │                         │ REST            │ Kafka consumer
     ▼                         ▼                 ▼
FastAPI agent (8000)     Warehouse (8082)   Payment events
                          sunridge_warehouse        ▲
                                  ▲                  │
                                  │ REST             │ outbox publisher
                                  └── Commerce ──► Payment (8083, sunridge_payment)
                                                       ▲
Stripe.js / Stripe CLI ───────────────────────────────┘
```

| Component | Port | Database | Responsibility |
| --- | ---: | --- | --- |
| Commerce | 8080 | `sunridge_commerce` | identity, catalog, orders, checkout coordination, fraud demo |
| Warehouse | 8082 | `sunridge_warehouse` | inventory, reservations, movements, fulfillment, shipment |
| Payment | 8083 | `sunridge_payment` | Stripe PaymentIntents, webhooks, payment outbox |
| FastAPI agent | 8000 | — | AI orchestration and order tools |
| Redpanda | 9092 | — | Kafka-compatible event broker |

The legacy `ecommerce` database is kept unchanged as a local migration source.

## Local startup

Create the databases once:

```bash
mysql -u root -p < infrastructure/mysql/create-service-databases.sql
```

Start Redpanda:

```bash
docker compose -f docker-compose.infrastructure.yml up -d
```

Start each Spring Boot service with the `local` profile. Local credentials belong only in ignored `application-local.yml` files.

```bash
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run

cd services/warehouse-service
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run

cd services/payment-service
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

Start the frontend and AI service in separate terminals:

```bash
cd frontend
npm install
npm run dev

cd agent-service
source .venv/bin/activate
uvicorn app.main:app --reload --port 8000
```

Forward Stripe test webhooks to the Payment service, not Commerce:

```bash
stripe listen --forward-to localhost:8083/api/stripe/webhook
```

Set the CLI-provided webhook secret and Stripe test secret key only in `services/payment-service/src/main/resources/application-local.yml`, then restart Payment. Set the Stripe publishable test key in the ignored frontend local environment file.

## Checkout and payment flow

1. React sends `POST /api/checkout` to Commerce with a client-generated `checkoutId` and cart items.
2. Commerce creates or reuses the order, projects its products to Warehouse if needed, and synchronously reserves stock.
3. Commerce runs the demo fraud decision, then synchronously asks Payment to create a Stripe PaymentIntent. `checkoutId` is the Stripe idempotency key.
4. React confirms the PaymentIntent with Stripe Elements. Card data goes directly to Stripe.
5. Stripe CLI forwards the signed webhook to Payment.
6. Payment updates its payment row and inserts an outbox row in one transaction.
7. The outbox publisher emits `PaymentSucceeded` or `PaymentFailed` to the `payment-events` topic.
8. Commerce consumes the event and updates the order state. Warehouse consumes the same event independently and confirms/releases the reservation; a successful payment creates `READY_TO_PICK` fulfillment work.

This mixes synchronous REST for immediate checkout decisions with Kafka for state changes that multiple services react to. Consumers store processed event IDs, so at-least-once delivery is safe.

## Persistent cart

Commerce owns a persisted cart for each signed-in customer: `carts` and `cart_items` are stored in `sunridge_commerce`. The React bag now calls these authenticated endpoints:

```text
GET    /api/cart
POST   /api/cart/items
PATCH  /api/cart/items/{productId}
DELETE /api/cart/items/{productId}
DELETE /api/cart
```

Adding an item to a cart does **not** reserve stock. Warehouse creates an expiring inventory reservation only when checkout begins; payment failure and the reservation-expiry job release it safely.

## Shipment tracking demo

After a warehouse worker marks a fulfillment shipped, the customer can open **Account → Track shipment**. React calls Commerce, which verifies that the selected order belongs to the signed-in customer before calling Warehouse's internal shipment endpoint. The browser never queries Warehouse by an arbitrary tracking number.

Warehouse keeps a separate `shipment_tracking_events` ledger for carrier history. This is distinct from Kafka's `processed_events` inbox table. For local development, simulate FedEx-style updates through the local-profile-only endpoint:

```bash
curl -X POST http://localhost:8082/api/carriers/fedex/webhook/test \
  -H 'Content-Type: application/json' \
  -d '{"eventId":"demo-fedex-1002-la","trackingNumber":"DEMO-FEDEX-1002","status":"IN_TRANSIT","location":"Los Angeles, CA","description":"Arrived at FedEx location"}'
```

The production-shaped endpoint is `POST /api/carriers/fedex/webhook`; it validates an HMAC signature when a FedEx webhook security token is configured. The local test endpoint deliberately avoids any FedEx account or public HTTPS callback requirement.

## Commerce order state machine

Commerce owns the customer-facing order lifecycle. It uses typed states and business events rather than arbitrary status strings:

```text
PENDING_PAYMENT --PAYMENT_SUCCEEDED--> PAID
PENDING_PAYMENT --PAYMENT_FAILED-----> PAYMENT_FAILED
PENDING_PAYMENT --FRAUD_REVIEW_REQUIRED--> FRAUD_REVIEW
PENDING_PAYMENT --FRAUD_DECLINED----> FRAUD_DECLINED
FRAUD_REVIEW ---REVIEW_APPROVED------> PENDING_PAYMENT
FRAUD_REVIEW ---REVIEW_DECLINED------> FRAUD_DECLINED
```

Each transition has a guard and a local action. For example, a payment event must reference the order's expected PaymentIntent; a successful transition writes an `order_status_history` audit row. Cross-service side effects should be emitted through an outbox event rather than called directly from a state-machine action.

## Database migration

Commerce starts against `sunridge_commerce` and Flyway creates its schema on first startup. To copy existing demo users, catalog, orders, order items, and fraud assessments from the legacy database, run:

```bash
mysql -u root -p < infrastructure/mysql/migrate-ecommerce-to-sunridge-commerce.sql
```

The script is idempotent and does not modify `ecommerce`. It intentionally does not copy old inventory, reservation, fulfillment, or shipment records because Warehouse is the new owner of those records.

## Test card

Use Stripe test card `4242 4242 4242 4242`, any future expiry date, and any three-digit CVC. Never use real card details in test mode.

## Further reading

- [Microservice migration details](MICROSERVICE_MIGRATION.md)
- [Payment process flow](PAYMENT_PROCESS_FLOW.md)
- [Google OAuth and application JWT flow](GOOGLE_OAUTH_JWT_FLOW.md)
- [Project handoff](PROJECT_HANDOFF.md)
