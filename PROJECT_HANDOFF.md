# E-commerce Platform Handoff

## Current state

The working demo has been split into three Spring Boot service domains while the Commerce service remains in the historical `backend/` folder during the migration:

- **Commerce** — `backend/`, port `8080`, database `sunridge_commerce`
- **Warehouse** — `services/warehouse-service/`, port `8082`, database `sunridge_warehouse`
- **Payment** — `services/payment-service/`, port `8083`, database `sunridge_payment`
- **React UI** — `frontend/`, port `5173`
- **FastAPI agent** — `agent-service/`, port `8000`
- **Redpanda** — Kafka-compatible broker on `9092`

`ecommerce` is retained only as the legacy local database. It is not modified by the new Commerce datasource or the optional copy script.

## Service communication

| From | To | Style | Purpose |
| --- | --- | --- | --- |
| React | Commerce | REST | login, catalog, checkout, orders |
| React | Warehouse | REST | operations UI: inventory, adjustments, fulfillment shipping |
| Commerce | Warehouse | REST | product projection and stock reservation |
| Commerce | Payment | REST | create/reuse Stripe PaymentIntent |
| Payment | Commerce and Warehouse | Kafka `payment-events` | final payment state |

Payment uses an outbox table and a scheduled publisher. Commerce and Warehouse consume with different consumer groups and each records event IDs in `processed_events` for idempotency.

## Startup order

1. Start local MySQL.
2. Start Redpanda: `docker compose -f docker-compose.infrastructure.yml up -d`.
3. Start Warehouse and Payment with the `local` Spring profile.
4. Start Commerce with the `local` Spring profile.
5. Run `stripe listen --forward-to localhost:8083/api/stripe/webhook`.
6. Start React and FastAPI as needed.

All real credentials are only in ignored local files or environment variables:

- `backend/src/main/resources/application-local.yml`
- `services/warehouse-service/src/main/resources/application-local.yml`
- `services/payment-service/src/main/resources/application-local.yml`
- `agent-service/.env`
- `frontend/.env.local`

## Current limitations and next work

- Commerce source still contains legacy local Warehouse code. Remove it after the extracted services are fully verified and Commerce is moved/renamed under `services/`.
- Browser traffic has no API gateway yet; React directly calls Commerce and the Warehouse operations API.
- Payment webhook-event persistence/deduplication should be completed in addition to the current payment-status guard.
- Add service-to-service authentication, staff roles for Warehouse, retries/dead-letter topics, observability, and CI/CD before a production deployment.
- The cart remains in browser storage; introduce persistent cart tables later.
