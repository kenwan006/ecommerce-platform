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

This first version deliberately uses a simple login response rather than production JWT sessions; add JWT/OAuth before deploying publicly.
