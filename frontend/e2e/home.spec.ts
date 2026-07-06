import { expect, test } from '@playwright/test';

/**
 * SPEC-0005 (Home, slice 1.4 — closes Phase 1) end to end over the real stack: login as MARIA
 * lands on the new Home default screen (AC1); switching the active beneficiary to PEDRO in the
 * shell selector updates the card immediately (AC5 — this phase's title journey); the notices
 * accordion allows only one open at a time (AC4); shortcuts/banners whose destination module is
 * not yet delivered render disabled with the "em breve" hint (BR4/BR6, phased-delivery note —
 * AC2 deferred to Phase 2). AC6 (the fraud banner → antifraud anchor) closes in Phase 5/SPEC-0014
 * — see the dedicated assertion below and `e2e/atendimento.spec.ts`.
 */
test('login → Home shows MARIA, switching to PEDRO updates the card, notices single-open, "em breve" shortcuts/banners', async ({
  page,
}) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();

  // AC1: greeting, plan name and card number of the active (default TITULAR) beneficiary.
  await expect(page.getByTestId('home-page')).toBeVisible();
  await expect(page.getByTestId('card-greeting')).toContainText('Olá, MARIA');
  await expect(page.getByTestId('card-plan-name')).toContainText('ADESÃO PRATA RJ QP COPART TP');
  await expect(page.getByTestId('card-number')).toContainText('001234567');

  // AC5 — this phase's title journey: switching to PEDRO updates the card immediately.
  await page.getByRole('combobox', { name: 'Beneficiário ativo' }).click();
  await page.getByRole('option', { name: /PEDRO/ }).click();
  await expect(page.getByTestId('card-greeting')).toContainText('Olá, PEDRO');
  await expect(page.getByTestId('card-number')).toContainText('001234575');

  // AC4: notices accordion, single-open — BR9 seed ships Telemedicina (ALERT, priority 1, open
  // by default) and LGPD (INFORMATIVE, priority 2); opening LGPD closes Telemedicina.
  const telemedicina = page.getByText('Instabilidade momentânea da Telemedicina');
  const lgpd = page.getByText('Lei Geral de Proteção de Dados Pessoais');
  await expect(telemedicina).toBeVisible();
  await lgpd.click();
  await expect(page.getByTestId('notice-2')).toHaveAttribute('data-p-active', 'true');
  await expect(page.getByTestId('notice-1')).toHaveAttribute('data-p-active', 'false');

  // BR4 (phased-delivery note): a not-yet-delivered shortcut renders disabled with "em breve"
  // (AC2 deferred — Carteirinha lands in Phase 2).
  await expect(page.getByTestId('shortcut-carteirinha')).toBeDisabled();
  await expect(page.getByTestId('shortcut-carteirinha-em-breve')).toContainText('Em breve');

  // BR6 extended to banner buttons (phased-delivery note): a banner whose destination module is
  // still undelivered ("Valide seu boleto" → finance, not in this phase) stays disabled with
  // "em breve". The carousel is [circular] (PrimeNG clones slides for looping) AND only the
  // currently active slide is exposed to the accessibility tree (inactive/cloned slides are
  // `aria-hidden`) — so the disabled banner is asserted via `data-testid`, not `getByRole`, which
  // would see 0 matches while that slide isn't active.
  await expect(page.getByText('Alerta de golpe!').first()).toBeVisible();
  const boletoButton = page.locator('[data-testid="banner-button"]', { hasText: 'Validar boleto' }).first();
  await expect(boletoButton).toBeDisabled();
  await expect(boletoButton).toContainText('Em breve');

  // AC6 (BR9), closed by SPEC-0014: the fraud banner's action IS enabled now that Canais de
  // Atendimento landed (Phase 5) — clicking it lands positioned at the antifraud section. It is
  // the initially active slide, so `getByRole` (the accessibility-exposed one) reliably finds it.
  // See `e2e/atendimento.spec.ts` for the FAQ/Libras/WhatsApp journeys of that same screen.
  const saibaMais = page.getByRole('button', { name: 'Saiba mais' }).first();
  await expect(saibaMais).toBeEnabled();
  await saibaMais.click();
  await expect(page).toHaveURL(/\/atendimento#antifraude$/);
  await expect(page.getByTestId('antifraude-titulo')).toBeVisible();
});
