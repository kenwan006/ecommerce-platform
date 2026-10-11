import { expect, test } from '@playwright/test';
import {
  authenticatePage,
  bearer,
  commerceUrl,
  productWithStock,
  registerCustomer,
} from './helpers.js';

test.skip(!process.env.E2E_RUN_LIVE, 'Set E2E_RUN_LIVE=true after starting the local services.');

test('checkout is rejected when requested quantity exceeds available inventory', async ({ page, request }) => {
  const customer = await registerCustomer(request);
  const { product, inventory } = await productWithStock(request);
  await authenticatePage(page, customer);

  await page.getByRole('button', { name: `Add ${product.name} to bag` }).click();
  const tooManyItems = inventory.availableQuantity + 1;
  const updateCart = await request.patch(`${commerceUrl}/api/cart/items/${product.id}`, {
    headers: bearer(customer),
    data: { quantity: tooManyItems },
  });
  expect(updateCart, await updateCart.text()).toBeOK();

  await page.getByRole('button', { name: /Bag/ }).click();
  await page.reload();
  await expect(page.getByText(product.name)).toBeVisible();
  await page.getByRole('button', { name: 'Continue to secure payment' }).click();

  await expect(page.getByText(/insufficient|available|inventory/i)).toBeVisible();
  await expect(page.getByText('YOUR BAG')).toBeVisible();
  await expect(page.getByText('SECURE CHECKOUT')).not.toBeVisible();
});
