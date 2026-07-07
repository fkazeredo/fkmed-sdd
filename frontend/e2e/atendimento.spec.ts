import { expect, Page, test } from '@playwright/test';

/**
 * SPEC-0014 (Canais de Atendimento e FAQ, Phase 5 — closes the phase) end to end over the real
 * stack, against the V25 seed: the Home fraud banner deep-links to the antifraud section (AC2),
 * the WhatsApp channel card opens the official number in a new tab (AC5/BR1), the FAQ real-time
 * search filters and restores the full list (AC1) with a single-open accordion (AC3), and
 * "Solicitar atendimento em Libras" registers and confirms a next step (AC4).
 */

async function login(page: Page): Promise<void> {
  await page.goto('/');
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();
}

test('Home banner → antifraude anchor; WhatsApp link; FAQ search/category/accordion; Libras confirmation', async ({
  page,
}) => {
  await login(page);

  // AC2: the Home fraud banner opens Atendimento positioned at the antifraud section. The
  // carousel is [circular] (PrimeNG clones the slide for looping) and there is a second banner
  // ("Valide seu boleto") — scope to the card containing the fraud banner's own title first.
  const fraudBanner = page.locator('[data-testid="banner-item"]', { hasText: 'Alerta de golpe!' }).first();
  await fraudBanner.getByTestId('banner-button').click();
  await expect(page.getByTestId('atendimento-hub-page')).toBeVisible();
  await expect(page.getByTestId('antifraude')).toBeInViewport();
  await expect(page.getByTestId('antifraude')).toContainText('Alerta de golpe!');

  // AC5/BR1: the WhatsApp card opens the official number's chat in a new tab.
  const whatsapp = page.getByTestId('canal-whatsapp').getByRole('link');
  await expect(whatsapp).toHaveAttribute('href', /^https:\/\/wa\.me\/\d+$/);
  await expect(whatsapp).toHaveAttribute('target', '_blank');

  // BR1: Central 24h groups both numbers (capitais + demais localidades) as tel: links.
  const centralLinks = page.getByTestId('canal-central').getByRole('link');
  await expect(centralLinks).toHaveCount(2);

  // Navigate to the FAQ.
  await page.getByTestId('link-faq').click();
  await expect(page.getByTestId('faq-page')).toBeVisible();

  // AC1: searching "reembolso" filters to only Reembolso questions.
  await page.getByTestId('faq-busca').fill('reembolso');
  await expect(page.locator('[data-testid^="faq-item-"]').first()).toBeVisible();
  const items = page.locator('[data-testid^="faq-item-"]');
  const count = await items.count();
  for (let i = 0; i < count; i++) {
    await expect(items.nth(i)).toContainText(/reembolso/i);
  }

  // Clearing the search restores the full list (>= 12 seeded entries, BR6).
  await page.getByTestId('faq-busca').fill('');
  await expect(async () => {
    expect(await items.count()).toBeGreaterThanOrEqual(12);
  }).toPass();

  // AC3: opening a second question closes the first one (single-open accordion).
  const firstHeader = page.locator('p-accordion-header').first();
  const secondHeader = page.locator('p-accordion-header').nth(1);
  await firstHeader.click();
  await expect(items.first()).toHaveAttribute('data-p-active', 'true');
  await secondHeader.click();
  await expect(items.first()).toHaveAttribute('data-p-active', 'false');
  await expect(items.nth(1)).toHaveAttribute('data-p-active', 'true');

  // A search with no matches shows the empty state (BR5).
  await page.getByTestId('faq-busca').fill('termo-sem-nenhum-resultado-possivel');
  await expect(page.getByTestId('faq-vazio')).toContainText("Nenhum resultado para 'termo-sem-nenhum-resultado-possivel'");

  // AC4: Central de Libras registers the request and confirms a next step.
  await page.goto('/atendimento/libras');
  await expect(page.getByTestId('libras-page')).toBeVisible();
  await page.getByTestId('libras-solicitar').click();
  await expect(page.getByTestId('libras-confirmacao')).toContainText(/videochamada em instantes|próximo período/);
});
