# Simple Ecommerce Platform

A React storefront and Spring Boot API backed by MySQL.

## Run locally

Start MySQL locally before starting the backend. Docker is optional; this project also works with a locally installed MySQL server.

Start the Spring Boot API with the ignored local configuration profile:

```bash
cd backend
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

Start the AI agent:

```bash
cd agent-service
source .venv/bin/activate
python -m uvicorn app.main:app --reload --port 8000
```

Create the virtual environment and install dependencies first if it does not exist:

```bash
cd agent-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Start the React UI:

```bash
cd frontend
npm install
npm run dev
```

Start the Stripe webhook listener in a separate terminal and leave it running while testing payments:

```bash
stripe listen \
  --events payment_intent.succeeded,payment_intent.payment_failed \
  --forward-to localhost:8080/api/stripe/webhook
```

Stripe CLI prints a webhook signing secret when it starts. Save that value only in the ignored local backend configuration (`backend/src/main/resources/application-local.yml`) or as `STRIPE_WEBHOOK_SECRET`, then restart Spring Boot if it changed.

The Spring API runs at `http://localhost:8080/api`, the FastAPI agent at `http://localhost:8000/api`, and the UI at `http://localhost:5173`.

## API overview

- `GET /api/products` — catalogue
- `POST /api/auth/register` — create an account
- `POST /api/auth/login` — validate credentials
- `GET /api/orders` — authenticated customer's order history
- `POST /api/checkout` — create a pending order and Stripe PaymentIntent
- `POST /api/stripe/webhook` — receive Stripe payment status events
- `GET /api/orders/{orderId}` — authenticated customer's order details
- `GET /api/orders/pending-payments` — authenticated customer's orders with pending payment
- `GET /api/warehouse/inventory` — inventory position for the warehouse page
- `GET /api/warehouse/inventory/movements` — latest stock movement ledger entries
- `POST /api/warehouse/inventory/movements` — import or export stock manually
- `GET /api/warehouse/fulfillments` — paid orders ready to ship
- `POST /api/warehouse/fulfillments/{fulfillmentId}/ship` — create a shipment and deduct inventory

## AI order assistant

The React chatbox calls the FastAPI service, which uses OpenAI function calling. When the model needs an order's status, it calls the Python `get_order_status` tool. The tool forwards the customer's JWT to Spring's `GET /api/orders/{orderId}` endpoint. Spring validates the JWT and returns an order only when it belongs to that customer.

FastAPI loads the public OpenAI documentation MCP tools at startup and makes them available to the existing chat agent. The chat can therefore answer both order questions and OpenAI API documentation questions. The MCP tools can also be inspected directly at `GET /api/mcp/openai-docs/tools` or searched at `POST /api/mcp/openai-docs/search`.

Create `agent-service/.env` from `agent-service/.env.example` and set `OPENAI_API_KEY` before starting FastAPI. Never commit this file or the key.

## Stripe test setup

Set a Stripe test secret key before starting the API, and a publishable test key before starting Vite:

```bash
export STRIPE_SECRET_KEY=sk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...
export VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...
```

Alternatively, copy `frontend/.env.example` to `frontend/.env.local` for Vite. Never commit real Stripe keys.

Use these cards only while the app is configured with `sk_test_...` and `pk_test_...` keys:

| Scenario | Card number | Expiry | CVC |
| --- | --- | --- | --- |
| Successful Visa payment | `4242 4242 4242 4242` | Any future date | Any three digits |
| Generic card decline | `4000 0000 0000 0002` | Any future date | Any three digits |

Do not enter a real card number in Stripe test mode or use a Stripe test card with live keys.

The UI sends `userId`, one client-generated `checkoutId`, and `{ productId, quantity }` items to `/api/checkout`. The API creates a `PENDING` order first, calculates the amount from its own database, and creates a Stripe PaymentIntent linked to that order. The `checkoutId` is used as Stripe's idempotency key, so retries do not create duplicate PaymentIntents. Card data is sent directly from Stripe Elements to Stripe, never to this API.

## Inventory and shipping demo

Inventory is now held through an explicit reservation rather than decrementing product stock during checkout:

1. Checkout reserves available units for 15 minutes.
2. Fraud decline, Stripe payment failure, or reservation expiry releases the held units.
3. The successful Stripe webhook marks the reservation confirmed and creates a `READY_TO_PICK` fulfillment order.
4. Open **Warehouse** in the React navigation. Enter a tracking number and select **Mark shipped**. This creates a shipment, changes the order to `SHIPPED`, and deducts both the reserved and on-hand inventory.

`inventory_levels` holds the current balance. `inventory_movements` is an append-only audit ledger: it records initial stock, imports, exports, and shipments. The Warehouse page can import new stock or export unreserved stock and shows the latest movements.

The initial migration creates a `MAIN` warehouse and seeds its on-hand inventory from the existing product `stock` values. The warehouse endpoints are intentionally open to any authenticated demo user; add staff roles and authorization rules before treating this as a production operations UI.

## Google sign-in (local demo)

Create a Google OAuth 2.0 **Web application** client and add this authorized redirect URI:

```
http://localhost:8080/login/oauth2/code/google
```

From the `backend` directory, copy the local configuration template and add the client credentials. This file is ignored by Git:

```
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

Then start the backend with the `local` profile:

```
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run
```

The React app's **Continue with Google** button starts the authorization-code flow at the backend. After Google authenticates the user, the backend creates or links a local user by Google subject/email and redirects to `http://localhost:5173` with a one-hour JWT in the URL fragment. React stores the token locally and sends it as a bearer token to `/api/auth/me` and protected APIs.

See [Google OAuth and JWT flow](GOOGLE_OAUTH_JWT_FLOW.md) for the full request-by-request sequence.

Generate a local JWT secret before starting the backend, then set it in `application-local.yml`:

```
openssl rand -base64 32
```

This first version deliberately uses a simple login response rather than production JWT sessions; add JWT/OAuth before deploying publicly.
