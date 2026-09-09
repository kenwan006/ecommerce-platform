# Simple Ecommerce Platform

A React storefront and Spring Boot API backed by MySQL.

## Run locally

1. Start MySQL: `docker compose up -d`
2. Start the Spring API: `cd backend && mvn spring-boot:run`
3. Start the AI agent (Python 3.12+): `cd agent-service && python3.12 -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt && uvicorn app.main:app --reload --port 8000`
4. Start the UI: `cd frontend && npm install && npm run dev`

The Spring API runs at `http://localhost:8080/api`, the FastAPI agent at `http://localhost:8000/api`, and the UI at `http://localhost:5173`.

## API overview

- `GET /api/products` — catalogue
- `POST /api/auth/register` — create an account
- `POST /api/auth/login` — validate credentials
- `POST /api/orders` — view/create order records
- `POST /api/checkout` — create a pending order and Stripe PaymentIntent
- `POST /api/stripe/webhook` — receive Stripe payment status events
- `GET /api/orders` — authenticated customer's order history
- `GET /api/orders/{orderId}` — authenticated customer's order details
- `GET /api/orders/pending-payments` — authenticated customer's orders with pending payment

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

The UI sends `userId`, one client-generated `checkoutId`, and `{ productId, quantity }` items to `/api/checkout`. The API creates a `PENDING` order first, calculates the amount from its own database, and creates a Stripe PaymentIntent linked to that order. The `checkoutId` is used as Stripe's idempotency key, so retries do not create duplicate PaymentIntents. Stripe's signed webhook changes the order to `PAID` and decrements stock after `payment_intent.succeeded`. Card data is sent directly from Stripe Elements to Stripe, never to this API.

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
