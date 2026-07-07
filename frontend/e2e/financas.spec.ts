import { expect, Page, test } from '@playwright/test';

/**
 * SPEC-0013 (Plano › Finanças, Phase 5) end to end over the real stack, against the V24 seed.
 * Titular MARIA: the invoice tabs (open + overdue with the valor-atualizado breakdown, paid), the
 * boleto detail with "Copiar linha digitável" / "PIX copia-e-cola" (exact clipboard content) and the
 * 2nd-copy PDF (BR3), the antifraud validator (authentic seeded line with spaces + a not-recognized
 * line with the do-not-pay alert — BR4), the copay statement (BR5) and the IR/settlement PDF
 * downloads (BR6/BR7); then switching to the dependent PEDRO hides the nav entry and denies direct
 * access (BR1). Mirrors e2e/minha-saude.spec (PDF pattern) and the beneficiary switch of e2e/home.
 */

async function login(page: Page): Promise<void> {
  await page.goto('/');
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();
}

test('MARIA: invoices, copy line/PIX, 2nd copy, validator, copay, IR/settlement PDFs; PEDRO denied', async ({
  page,
}) => {
  await page.context().grantPermissions(['clipboard-read', 'clipboard-write']);
  await login(page);

  // Hub via the nav (hidden for dependents — asserted at the end).
  await page.getByTestId('nav-financas').click();
  await expect(page.getByTestId('financas-hub-page')).toBeVisible();

  // Em aberto: open + overdue, the overdue one flagged with the channels guidance + the breakdown.
  await expect(page.locator('[data-testid^="financas-boleto-"]').first()).toBeVisible({ timeout: 15000 });
  await expect(page.getByTestId('financas-atualizado').first()).toContainText(
    'Atualize seu boleto pelos canais de atendimento',
  );

  // Open the first boleto → detail; copy exactly the displayed 47-digit line and the PIX code.
  await page.locator('[data-testid^="financas-boleto-"] a').first().click();
  await expect(page.getByTestId('boleto-detail-page')).toBeVisible();
  const line = (await page.getByTestId('boleto-linha').innerText()).trim();
  expect(line).toMatch(/^\d{47}$/);

  await page.getByTestId('boleto-copiar-linha').click();
  await expect(page.getByTestId('boleto-linha-copiada')).toBeVisible();
  expect(await page.evaluate(() => navigator.clipboard.readText())).toBe(line);

  await page.getByTestId('boleto-copiar-pix').click();
  await expect(page.getByTestId('boleto-pix-copiado')).toBeVisible();
  expect(await page.evaluate(() => navigator.clipboard.readText())).toContain('00020126');

  // 2nd-copy PDF (BR3).
  const [boletoPdf] = await Promise.all([
    page.waitForEvent('download'),
    page.getByTestId('boleto-baixar-pdf').click(),
  ]);
  expect(boletoPdf.suggestedFilename()).toMatch(/\.pdf$/i);

  // Pagos tab: the prior-year paid invoices, with a payment date.
  await page.getByTestId('nav-financas').click();
  await expect(page.getByTestId('financas-hub-page')).toBeVisible();
  await page.getByTestId('financas-tab-pagos').click();
  await expect(page.getByTestId('financas-pago-em').first()).toBeVisible();

  // Validator (BR4): the seeded line with spaces → autêntico; 47 unknown digits → do-not-pay alert.
  await page.getByTestId('financas-hub-validar').click();
  await expect(page.getByTestId('validar-page')).toBeVisible();
  await page.getByTestId('validar-input').fill(line.replace(/(.{5})/g, '$1 ').trim());
  await page.getByTestId('validar-submit').click();
  await expect(page.getByTestId('validar-autentico')).toBeVisible();

  await page.getByTestId('validar-input').fill('1'.repeat(47));
  await page.getByTestId('validar-submit').click();
  const alert = page.getByTestId('validar-nao-reconhecido');
  await expect(alert).toBeVisible();
  await expect(alert).toContainText('Não realize o pagamento');

  // Copay (BR5): last 3 months shows the family's entries + the period total.
  await page.goto('/financas/coparticipacao');
  await expect(page.getByTestId('coparticipacao-page')).toBeVisible();
  await page.getByTestId('copay-periodo').selectOption('LAST_3M');
  await expect(page.getByTestId('copay-total')).toBeVisible({ timeout: 15000 });

  // IR statement PDF (BR6).
  await page.goto('/financas/imposto-renda');
  await expect(page.getByTestId('imposto-renda-page')).toBeVisible();
  await expect(page.locator('[data-testid^="ir-baixar-"]').first()).toBeVisible({ timeout: 15000 });
  const [irPdf] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('[data-testid^="ir-baixar-"]').first().click(),
  ]);
  expect(irPdf.suggestedFilename()).toMatch(/\.pdf$/i);

  // Settlement declaration PDF for the fully-paid year (BR7).
  await page.goto('/financas/quitacao');
  await expect(page.getByTestId('quitacao-page')).toBeVisible();
  await expect(page.locator('[data-testid^="quitacao-baixar-"]').first()).toBeVisible({ timeout: 15000 });
  const [settlementPdf] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('[data-testid^="quitacao-baixar-"]').first().click(),
  ]);
  expect(settlementPdf.suggestedFilename()).toMatch(/\.pdf$/i);

  // BR1: switch to the dependent PEDRO — the nav entry disappears and direct access is denied.
  await page.getByRole('combobox', { name: 'Beneficiário ativo' }).click();
  await page.getByRole('option', { name: /PEDRO/ }).click();
  await expect(page.getByTestId('nav-financas')).toHaveCount(0);

  await page.goto('/financas');
  await expect(page.getByTestId('financas-denied')).toBeVisible();
});
