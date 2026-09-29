# E-commerce Platform Handoff

## Services

- **React/Vite UI**: `http://localhost:5173`
- **Spring Boot API**: `http://localhost:8080`
- **FastAPI AI agent**: `http://localhost:8000`
- **MySQL**: local database, managed by Flyway migrations
- **Stripe CLI**: forwards payment webhooks to Spring Boot

## Local startup

```bash
# Spring Boot
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run

# React
cd frontend
npm run dev

# AI agent
cd agent-service
source .venv/bin/activate
python -m uvicorn app.main:app --reload --port 8000

# Stripe webhook listener
stripe listen \
  --events payment_intent.succeeded,payment_intent.payment_failed \
  --forward-to localhost:8080/api/stripe/webhook
```

Credentials are local-only and ignored by Git: Spring's `application-local.yml`, the agent `.env`, and frontend `.env.local`.

## Authentication

- Local email/password login creates the platform JWT.
- Google login is OIDC authorization-code flow through Spring Security.
- React navigates to `/oauth2/authorization/google`; Spring redirects to Google.
- On callback, Spring finds/creates the local user, creates a JWT, and redirects React with it in the URL fragment.
- React stores the JWT and sends it as `Authorization: Bearer ...`.

## Checkout, payment, fraud

1. React creates one `checkoutId` and sends it with cart items.
2. Spring creates/reuses the order by `checkoutId`; Stripe uses that ID as its idempotency key.
3. Inventory is reserved for 15 minutes before payment.
4. The demo fraud service assesses the order before the PaymentIntent is created.
5. Stripe Elements sends card data directly to Stripe.
6. Stripe webhook is signature-verified by Spring.
7. Payment success marks the order paid, confirms the reservation, and creates fulfillment work.
8. Payment failure, fraud decline, or reservation expiry releases the reservation.

The fraud ML scorer is a deliberately simple in-process logistic-regression demo. A production scorer should be a versioned HTTP/gRPC ML service with timeouts, fallback, and monitoring.

## Inventory and shipping

- `products` is the catalog/SKU-level product. This demo uses interchangeable stock, not one record per physical item.
- `inventory_levels` stores current `on_hand`, `reserved`, and available quantity for a product at a warehouse.
- `inventory_reservations` holds stock during checkout, then becomes confirmed after payment.
- `inventory_movements` is an append-only ledger for initial migration balance, manual imports, exports, and shipments.
- `fulfillment_orders` and `fulfillment_items` represent warehouse work for a paid customer order.
- `shipments` records carrier and tracking number.

Warehouse flow:

1. A paid order becomes `READY_TO_PICK`.
2. Warehouse UI shows it under **Orders ready to ship**.
3. Enter tracking number and select **Mark shipped**.
4. The order becomes `SHIPPED`; reserved and on-hand stock are reduced; a shipment and movement-ledger entry are created.

Warehouse UI supports manual imports and exports. Exports cannot consume stock reserved for customer orders.

## AI agent

- React chatbox calls FastAPI, not Spring Boot.
- FastAPI uses an OpenAI agent with function tools.
- Order tools call Spring REST APIs and forward the user JWT.
- The agent also connects to OpenAI documentation and GitHub MCP servers.
- Agent setup is intentionally demo-oriented; more agents/routing can be added later if distinct responsibilities emerge.

## Main API areas

- `/api/auth/*` — local authentication and current user
- `/api/products` — catalog
- `/api/checkout` — idempotent checkout / Stripe PaymentIntent
- `/api/stripe/webhook` — Stripe events
- `/api/orders/*` — customer order access
- `/api/warehouse/inventory` — inventory balances
- `/api/warehouse/inventory/movements` — movement ledger; POST imports/exports
- `/api/warehouse/fulfillments` — ready-to-ship work
- `/api/warehouse/fulfillments/{id}/ship` — dispatch a shipment

## Important current limitations / good next work

- Warehouse endpoints require a JWT but have no staff/warehouse role authorization yet.
- Cart is only stored in browser local storage; add cart/cart-item tables to persist it per user.
- Add automated tests for inventory reservation, movement ledger, and webhook-to-fulfillment flow.
- Introduce a payment-provider abstraction before adding PayPal/Adyen.
- Add asynchronous processing/queues and Redis when higher payment throughput is needed.
- Add serial-number or lot tracking only for product categories that need traceability.
- Keep real API keys out of Git; this repository is configured to use ignored local files/environment variables.
