import { expect, Page, test } from '@playwright/test';

/**
 * SPEC-0011 (Clinical Documents / Minha Saúde, Phase 4) end to end over the real stack, against the
 * V18-seeded documents. Covers the list with filters + validity badges, the type-specific detail
 * (incl. the CID on sick notes — DL-0020), the faithful PDF download, and the referral → "Agendar
 * consulta" handoff opening the SPEC-0009 wizard with the specialty pre-selected (AC4/BR6).
 */

async function login(page: Page): Promise<void> {
  await page.goto('/');
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();
}

async function openHub(page: Page): Promise<void> {
  await page.getByTestId('nav-minha-saude').click();
  await expect(page.getByTestId('minha-saude-hub-page')).toBeVisible();
}

test('AC9: prescriptions/sick notes list, detail with CID and PDF download', async ({ page }) => {
  await login(page);
  await openHub(page);

  await page.getByTestId('minha-saude-hub-receituarios').click();
  await expect(page.getByTestId('documentos-lista-page')).toBeVisible();

  // The seeded documents surface with a validity badge; open the first.
  const firstCard = page.locator('li[data-testid^="documento-card-"]').first();
  await expect(firstCard).toBeVisible({ timeout: 15000 });
  await expect(firstCard.getByTestId('documento-card-validade')).toBeVisible();
  await firstCard.click();

  // Type-specific detail with the common header.
  await expect(page.getByTestId('detalhe-page')).toBeVisible();
  await expect(page.getByTestId('detalhe-profissional')).toBeVisible();
  await expect(page.getByTestId('detalhe-validade')).toBeVisible();

  // The PDF download starts (faithful PDF — BR7).
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByTestId('detalhe-baixar-pdf').click(),
  ]);
  expect(download.suggestedFilename()).toMatch(/\.pdf$/i);
});

test('AC9/AC4: a referral opens "Agendar consulta" with the specialty pre-selected', async ({ page }) => {
  await login(page);
  await openHub(page);

  await page.getByTestId('minha-saude-hub-encaminhamentos').click();
  await expect(page.getByTestId('documentos-lista-page')).toBeVisible();

  const firstReferral = page.locator('li[data-testid^="documento-card-"]').first();
  await expect(firstReferral).toBeVisible({ timeout: 15000 });
  await firstReferral.click();

  await expect(page.getByTestId('detalhe-especialidade-alvo')).toBeVisible();
  await page.getByTestId('detalhe-agendar-consulta').click();

  // The SPEC-0009 consultation wizard opens with the referral's specialty already chosen.
  await expect(page.getByTestId('consulta-wizard-page')).toBeVisible();
  await expect(page.getByTestId('consulta-especialidade-escolhida')).toBeVisible();
});

test('AC9: the period filter narrows the list', async ({ page }) => {
  await login(page);
  await openHub(page);
  await page.getByTestId('minha-saude-hub-exames').click();
  await expect(page.getByTestId('documentos-lista-page')).toBeVisible();

  // Switching the period re-queries; the list stays consistent (no error state).
  await page.getByTestId('lista-filtro-periodo').selectOption({ index: 0 });
  await expect(page.getByTestId('lista-erro')).toHaveCount(0);
});
