# Microservice Migration Plan

The existing `backend` remains the working modular-monolith façade during migration. Do not delete it or switch the UI until extracted capabilities are verified.

## Target services and database ownership

| Service | Port | Database | Owns |
| --- | --- | --- | --- |
| Commerce (current backend during migration) | 8080 | `sunridge_commerce` | catalog, cart, order, checkout coordination, identity |
| Warehouse | 8082 | `sunridge_warehouse` | stock, reservations, movement ledger, fulfillment, shipment |
| Payment | 8083 | `sunridge_payment` | provider payments, webhook log, payment outbox |
| AI agent | 8000 | none | LLM orchestration and tool calls only |

Each service owns its tables. An identifier such as `commerce_order_id` is a cross-service reference, not a cross-database foreign key.

## Communication model

Synchronous APIs are for decisions that must complete before the caller responds:

- Commerce -> Warehouse: reserve inventory.
- Commerce -> Payment: create a payment session/PaymentIntent.

Kafka-compatible Redpanda events are for downstream state changes:

- `PaymentSucceeded` -> Commerce marks order paid; Warehouse confirms reservation and creates fulfillment.
- `PaymentFailed` -> Commerce marks order failed; Warehouse releases reservation.
- `ShipmentCreated` -> Commerce updates customer-visible order state; Notification sends tracking information.

The payment service must use the outbox pattern: store payment state and an outbox row in one transaction, then publish the row asynchronously. Consumers must be idempotent using the event ID.

## Migration sequence

1. Create the three schemas and start Redpanda.
2. Bring up Warehouse and Payment as independent applications with their own Flyway history.
3. Extract Warehouse reservation/fulfillment APIs and point Commerce at them.
4. Extract PaymentIntent/webhook processing; publish payment events.
5. Move Commerce data to `sunridge_commerce` and remove duplicated Warehouse/Payment code from the old backend.
6. Add Spring Cloud Gateway as the browser's single API endpoint after service APIs stabilize.

## Local infrastructure

Create empty schemas using the local MySQL account:

```bash
mysql -u root -p < infrastructure/mysql/create-service-databases.sql
```

Start Kafka-compatible Redpanda:

```bash
docker compose -f docker-compose.infrastructure.yml up -d
```

The current frontend continues to use `http://localhost:8080` until the gateway phase.
# Commerce database migration

Commerce now uses `sunridge_commerce`; the legacy `ecommerce` database remains unchanged.

1. Restart Commerce with the `local` profile. Flyway creates the Commerce schema in `sunridge_commerce`.
2. To retain the old demo's users, products, orders, order items, and fraud assessments, run the idempotent script:

   ```bash
   mysql -u root -p < infrastructure/mysql/migrate-ecommerce-to-sunridge-commerce.sql
   ```

The script deliberately does not copy the old inventory, reservations, fulfillment, or shipment tables. Warehouse is now the owner of those records.
