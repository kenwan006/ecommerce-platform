import { expect } from '@playwright/test';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

export const commerceUrl = process.env.E2E_COMMERCE_URL || 'http://localhost:8080';
export const warehouseUrl = process.env.E2E_WAREHOUSE_URL || 'http://localhost:8082';
const execFileAsync = promisify(execFile);

/**
 * Confirms a Stripe test PaymentIntent without driving Stripe's anti-bot protected hosted fields.
 * The browser still creates the checkout through the real UI; Stripe CLI must already be logged in.
 */
export async function confirmStripeTestPayment(paymentIntentId) {
  await execFileAsync(
    'stripe',
    [
      'payment_intents',
      'confirm',
      paymentIntentId,
      '--payment-method',
      'pm_card_visa',
      '--return-url',
      process.env.E2E_RETURN_URL || 'http://localhost:5173',
    ],
    { timeout: 30_000 },
  );
}

export async function registerCustomer(request) {
  const nonce = `${Date.now()}-${Math.floor(Math.random() * 1_000_000)}`;
  const response = await request.post(`${commerceUrl}/api/auth/register`, {
    data: {
      name: `E2E Customer ${nonce}`,
      email: `e2e-${nonce}@example.test`,
      password: 'E2e-test-password!',
    },
  });

  expect(response, await response.text()).toBeOK();
  return response.json();
}

export async function authenticatePage(page, customer) {
  await page.goto('/');
  await page.evaluate(({ accessToken, userId, name, email }) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('user', JSON.stringify({ userId, name, email }));
  }, customer);
  await page.reload();
}

export function bearer(customer) {
  return { Authorization: `Bearer ${customer.accessToken}` };
}

export async function productWithStock(request, minimumAvailable = 1) {
  const [productsResponse, inventoryResponse] = await Promise.all([
    request.get(`${commerceUrl}/api/products`),
    request.get(`${warehouseUrl}/api/warehouse/inventory`),
  ]);
  expect(productsResponse, await productsResponse.text()).toBeOK();
  expect(inventoryResponse, await inventoryResponse.text()).toBeOK();

  const products = await productsResponse.json();
  const inventory = await inventoryResponse.json();
  const level = inventory.find(item => item.availableQuantity >= minimumAvailable);
  const product = products.find(item => item.id === level?.productId);
  if (!product) {
    throw new Error(`No product has at least ${minimumAvailable} available units. Seed warehouse inventory first.`);
  }
  return { product, inventory: level };
}

export async function orderForCustomer(request, orderId, customer) {
  const response = await request.get(`${commerceUrl}/api/orders/${orderId}`, {
    headers: bearer(customer),
  });
  expect(response, await response.text()).toBeOK();
  return response.json();
}

export async function waitForPaidOrder(request, orderId, customer) {
  await expect.poll(
    async () => (await orderForCustomer(request, orderId, customer)).status,
    { timeout: 60_000, message: `order ${orderId} should become PAID after the Stripe webhook and Kafka event` },
  ).toBe('PAID');

  return orderForCustomer(request, orderId, customer);
}

export async function waitForFulfillment(request, orderId) {
  await expect.poll(
    async () => {
      const response = await request.get(`${warehouseUrl}/api/warehouse/fulfillments`);
      if (!response.ok()) return undefined;
      const fulfillments = await response.json();
      return fulfillments.find(item => item.orderId === orderId);
    },
    { timeout: 60_000, message: `a warehouse fulfillment should be created for paid order ${orderId}` },
  ).not.toBeUndefined();

  const response = await request.get(`${warehouseUrl}/api/warehouse/fulfillments`);
  return (await response.json()).find(item => item.orderId === orderId);
}
