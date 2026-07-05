import { expect, test } from '@playwright/test';

/**
 * SPEC-0005 (Home, slice 1.4 — closes Phase 1) end to end over the real stack: login as MARIA
 * lands on the new Home default screen (AC1); switching the active beneficiary to PEDRO in the
 * shell selector updates the card immediately (AC5 — this phase's title journey); the notices
 * accordion allows only one open at a time (AC4); shortcuts/banners whose destination module is
 * not yet delivered render disabled with the "em breve" hint (BR4/BR6, phased-delivery note —
 * AC2/AC6 deferred to Phase 2/5).
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

  // BR6 extended to banner buttons (phased-delivery note): the fraud-alert banner's CTA is
  // disabled with "em breve" too (AC6 deferred — Canais de Atendimento/antifraude lands Phase 5).
  await expect(page.getByText('Alerta de golpe!')).toBeVisible();
  await expect(page.getByTestId('banner-em-breve').first()).toContainText('Em breve');
  await expect(page.getByTestId('banner-button').first()).toBeDisabled();
});
