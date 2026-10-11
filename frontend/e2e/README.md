# Storefront end-to-end tests

The tests use **Playwright** against the real local services. They are deliberately opt-in: no test creates an order, touches inventory, or charges a Stripe test PaymentIntent unless `E2E_RUN_LIVE=true` is set.

## Prerequisites

Start the local dependencies and services first:

1. Redpanda/Kafka on `localhost:9092`.
2. Commerce on `8080`, Warehouse on `8082`, and Payment on `8083`.
3. The Stripe webhook forwarder, pointing at Payment:

   ```bash
   stripe listen --forward-to localhost:8083/api/stripe/webhook
   ```

4. The storefront on `5173`, with `VITE_STRIPE_PUBLISHABLE_KEY` configured.

Install the Playwright browser once:

```bash
cd frontend
npx playwright install chromium
```

## Run

```bash
cd frontend
E2E_RUN_LIVE=true npm run test:e2e
```

The happy-path test creates checkout in the browser, then confirms its PaymentIntent with Stripe CLI's `pm_card_visa` test payment method. Stripe's hosted Payment Element uses anti-bot controls that make headless-card entry unreliable. The test still asserts Payment webhook/Kafka propagation to a `PAID` order, Warehouse fulfillment creation, warehouse shipment dispatch, customer return request and demo label, sellable-item restock after inspection, and the resulting `REFUNDED` order/payment status.

The unhappy-path test deliberately places more items in the saved cart than Warehouse has available. It asserts that checkout stays in the bag and does not open Stripe's payment form.

`E2E_BASE_URL`, `E2E_COMMERCE_URL`, and `E2E_WAREHOUSE_URL` can override the default local addresses.
