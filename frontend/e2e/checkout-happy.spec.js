import { expect, test } from '@playwright/test';
import {
  authenticatePage,
  commerceUrl,
  confirmStripeTestPayment,
  orderForCustomer,
  productWithStock,
  registerCustomer,
  waitForFulfillment,
  waitForPaidOrder,
  warehouseUrl,
} from './helpers.js';

test.skip(!process.env.E2E_RUN_LIVE, 'Set E2E_RUN_LIVE=true after starting the local services and Stripe webhook forwarder.');

test('customer can pay, ship a return, and receive a refund after warehouse inspection', async ({ page, request }) => {
  const customer = await registerCustomer(request);
  const { product, inventory } = await productWithStock(request);
  await authenticatePage(page, customer);

  await page.getByRole('button', { name: `Add ${product.name} to bag` }).click();
  await expect(page.getByText(`${product.name} added to cart.`)).toBeVisible();
  await page.getByRole('button', { name: /Bag/ }).click();

  const checkoutResponse = page.waitForResponse(response =>
    response.url() === `${commerceUrl}/api/checkout` && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Continue to secure payment' }).click();
  const checkout = await (await checkoutResponse).json();
  const orderId = checkout.orderId;
  await expect(page.getByText('SECURE CHECKOUT')).toBeVisible();

  await confirmStripeTestPayment(checkout.paymentIntentId);

  const paidOrder = await waitForPaidOrder(request, orderId, customer);
  expect(paidOrder.paymentStatus).toBe('PAID');
  const fulfillment = await waitForFulfillment(request, orderId);

  await page.getByRole('button', { name: 'Warehouse' }).click();
  await expect(page.getByRole('heading', { name: 'Inventory & shipping' })).toBeVisible();
  const trackingNumber = `E2E-TRACK-${orderId}`;
  const fulfillmentRow = page.locator('.fulfillment').filter({ hasText: `Order #${orderId}` });
  await fulfillmentRow.getByLabel(`Tracking number for order ${orderId}`).fill(trackingNumber);
  await fulfillmentRow.getByRole('button', { name: 'Mark shipped' }).click();
  await expect.poll(
    async () => {
      const response = await request.get(`${commerceUrl}/api/orders/${orderId}/shipment`, {
        headers: { Authorization: `Bearer ${customer.accessToken}` },
      });
      return response.ok() ? (await response.json()).trackingNumber : undefined;
    },
    { timeout: 20_000 },
  ).toBe(trackingNumber);

  const inventoryAfterShipResponse = await request.get(`${warehouseUrl}/api/warehouse/inventory`);
  expect(inventoryAfterShipResponse, await inventoryAfterShipResponse.text()).toBeOK();
  const inventoryAfterShip = await inventoryAfterShipResponse.json();
  const productAfterShip = inventoryAfterShip.find(item => item.productId === product.id);
  expect(productAfterShip.onHandQuantity).toBe(inventory.onHandQuantity - 1);
  expect(fulfillment.status).toBe('READY_TO_PICK');

  await page.getByRole('button', { name: 'Track shipment' }).click();
  await page.getByRole('button', { name: `Order #${orderId}` }).click();
  await expect(page.getByText(trackingNumber)).toBeVisible();

  const returnResponse = page.waitForResponse(response =>
    response.url() === `${commerceUrl}/api/orders/${orderId}/returns`
      && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Request return' }).click();
  const returnRequest = await (await returnResponse).json();
  expect(returnRequest.status).toBe('APPROVED');
  await expect(page.getByText(returnRequest.rmaNumber).first()).toBeVisible();

  const inspection = await request.post(
    `${commerceUrl}/internal/returns/${returnRequest.id}/inspect?disposition=SELLABLE&note=E2E%20inspection`,
  );
  expect(inspection, await inspection.text()).toBeOK();
  expect((await inspection.json()).status).toBe('RECEIVED');

  await expect.poll(
    async () => (await orderForCustomer(request, orderId, customer)).status,
    { timeout: 20_000 },
  ).toBe('REFUNDED');
  const refundedOrder = await orderForCustomer(request, orderId, customer);
  expect(refundedOrder.paymentStatus).toBe('REFUNDED');

  const inventoryAfterReturnResponse = await request.get(`${warehouseUrl}/api/warehouse/inventory`);
  expect(inventoryAfterReturnResponse, await inventoryAfterReturnResponse.text()).toBeOK();
  const inventoryAfterReturn = await inventoryAfterReturnResponse.json();
  const productAfterReturn = inventoryAfterReturn.find(item => item.productId === product.id);
  expect(productAfterReturn.onHandQuantity).toBe(inventory.onHandQuantity);
});
