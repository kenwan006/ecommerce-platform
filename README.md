# Simple Ecommerce Platform

A React storefront and Spring Boot API backed by MySQL.

## Run locally

1. Start MySQL: `docker compose up -d`
2. Start the API: `cd backend && mvn spring-boot:run`
3. Start the UI: `cd frontend && npm install && npm run dev`

The API runs at `http://localhost:8080/api`; the UI runs at `http://localhost:5173`.

## API overview

- `GET /api/products` — catalogue
- `POST /api/auth/register` — create an account
- `POST /api/auth/login` — validate credentials
- `POST /api/orders` — view/create order records
- `POST /api/checkout` — create a pending order and Stripe PaymentIntent
- `POST /api/stripe/webhook` — receive Stripe payment status events
- `GET /api/orders/user/{userId}` — order history

## Stripe test setup

Set a Stripe test secret key before starting the API, and a publishable test key before starting Vite:

```bash
export STRIPE_SECRET_KEY=sk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...
export VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...
```

Alternatively, copy `frontend/.env.example` to `frontend/.env.local` for Vite. Never commit real Stripe keys.

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
