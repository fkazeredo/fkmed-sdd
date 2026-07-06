import { expect, Page, test } from '@playwright/test';

/**
 * SPEC-0014 (Canais de Atendimento, Phase 5) end to end over the real stack, against the
 * V25-seeded channels/FAQ. Covers the Home fraud banner landing on the antifraud anchor (AC2,
 * closing the deferred SPEC-0005 AC6), the FAQ real-time search + clear (AC1), the single-open
 * accordion (AC3), the Central de Libras request confirmation (AC4) and the WhatsApp card opening
 * the official chat in a new tab (AC5).
 */

async function login(page: Page): Promise<void> {
  await page.goto('/');
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();
}

async function openAtendimento(page: Page): Promise<void> {
  await page.getByTestId('nav-atendimento').click();
  await expect(page.getByTestId('atendimento-page')).toBeVisible();
}

test('AC2/SPEC-0005 AC6: the Home fraud banner lands positioned at the antifraud section', async ({ page }) => {
  await login(page);

  const saibaMais = page.getByRole('button', { name: 'Saiba mais' }).first();
  await expect(saibaMais).toBeEnabled();
  await saibaMais.click();

  await expect(page).toHaveURL(/\/atendimento#antifraude$/);
  await expect(page.getByTestId('antifraude-titulo')).toBeVisible();
  await expect(page.getByTestId('antifraude-titulo')).toContainText('Alerta de golpe!');
  await expect(page.getByTestId('antifraude-pratica-boleto')).toBeVisible();
});

test('AC1/BR5: FAQ search for "reembolso" filters the list; clearing restores the full list', async ({ page }) => {
  await login(page);
  await openAtendimento(page);

  const initialCount = await page.locator('[data-testid^="faq-item-"]').count();
  expect(initialCount).toBeGreaterThanOrEqual(12);

  await page.getByTestId('faq-busca').fill('reembolso');
  await expect(page.locator('[data-testid^="faq-item-"]')).toHaveCount(3, { timeout: 10000 });
  for (const question of await page.locator('[data-testid^="faq-item-"]').allTextContents()) {
    expect(question.toLowerCase()).toContain('reembolso');
  }

  await page.getByTestId('faq-busca').fill('');
  await expect(page.locator('[data-testid^="faq-item-"]')).toHaveCount(initialCount, { timeout: 10000 });
});

test('BR5: no matches shows the empty state with the search term', async ({ page }) => {
  await login(page);
  await openAtendimento(page);

  await page.getByTestId('faq-busca').fill('xyzxyznaoexiste');
  await expect(page.getByTestId('faq-vazio')).toBeVisible();
  await expect(page.getByTestId('faq-vazio')).toContainText("Nenhum resultado para 'xyzxyznaoexiste'");
});

test('AC3/BR5: opening a FAQ question closes the previously open one', async ({ page }) => {
  await login(page);
  await openAtendimento(page);

  const items = page.locator('[data-testid^="faq-item-"]');
  const first = items.nth(0);
  const second = items.nth(1);

  await first.locator('p-accordion-header').click();
  await expect(first).toHaveAttribute('data-p-active', 'true');

  await second.locator('p-accordion-header').click();
  await expect(second).toHaveAttribute('data-p-active', 'true');
  await expect(first).toHaveAttribute('data-p-active', 'false');
});

test('AC4/BR4: requesting Central de Libras registers and shows the confirmation', async ({ page }) => {
  await login(page);
  await openAtendimento(page);

  await page.getByTestId('libras-solicitar').click();
  await expect(page.getByTestId('libras-confirmacao')).toBeVisible();
  await expect(page.getByTestId('libras-mensagem')).toBeVisible();
});

test('AC5/BR1: the WhatsApp card opens the official-number chat in a new tab', async ({ page, context }) => {
  await login(page);
  await openAtendimento(page);

  const [popup] = await Promise.all([
    context.waitForEvent('page'),
    page.getByTestId('canal-WHATSAPP-link').click(),
  ]);
  await popup.waitForLoadState('domcontentloaded').catch(() => undefined);
  // wa.me redirects to WhatsApp's own domain, but the official number always survives the
  // redirect as the `phone` query param — assert on that rather than the (live, external,
  // occasionally-redirecting) host name.
  expect(popup.url()).toContain('5511987654321');
});
