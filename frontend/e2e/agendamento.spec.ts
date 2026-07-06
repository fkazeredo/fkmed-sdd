import { expect, Page, test } from '@playwright/test';

/**
 * SPEC-0009 (Appointments, Phase 3) end to end over the real stack: a full consultation booking
 * reaching a protocol that then appears in Próximos as AGENDADO (AC1), the exam wizard blocking the
 * advance when no medical order is attached (AC2/BR4), and cancel → the item moves to Histórico as
 * CANCELADO with a fresh rebook afterwards (AC4/BR9).
 *
 * NOTE for the architect: reconciled to the REAL backend contract (integration rework) — JSON
 * `POST /api/appointments` for consultations, multipart part `medicalOrder` for exams,
 * `GET /api/appointments` → `{upcoming,history}`, `GET /api/appointments/exams`. This cross-stack
 * spec runs at integration once both halves land; it is NOT part of this dev's own gate (lint +
 * unit + build only, per the work order). The exact specialty/exam registry codes, unit ids and
 * slot casing are backend-authored fixtures (SPEC-0009 §Persistence seed: 2 own units; a 30-day
 * Mon–Sat 08:00–17:00 30-min agenda; the exam_type catalog), so the selectors match the FIRST
 * available option / enabled slot rather than a hardcoded code.
 */

async function login(page: Page): Promise<void> {
  await page.goto('/');
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();
}

// Each booking must take a DISTINCT slot: `workers: 1` shares one database across the whole run
// (see playwright.config), so booking the same beneficiary at the same datetime twice would trip
// the BR8 time-conflict guard (409). This index walks successive open slots instead of always
// taking the first, keeping the tests independent of each other's leftovers.
let nextSlotIndex = 0;

async function bookConsultation(page: Page): Promise<string> {
  await page.getByTestId('nav-agendamento').click();
  await expect(page.getByTestId('agendamento-hub-page')).toBeVisible();
  await page.getByTestId('agendamento-hub-consulta').click();
  await expect(page.getByTestId('consulta-wizard-page')).toBeVisible();

  // Step 1 — specialty (registry shared with SPEC-0008); pick the first offered.
  await page.locator('[data-testid^="option-item-"]').first().click();
  await page.getByTestId('consulta-proximo').click();

  // Step 2 — unit (own units serving the specialty); pick the first.
  await expect(page.getByTestId('unit-picker')).toBeVisible();
  await page.locator('[data-testid^="unit-option-"]').first().click();
  await page.getByTestId('consulta-proximo').click();

  // Step 3 — date/time; pick the first day then a DISTINCT open slot per booking (full ones are
  // disabled, BR5) so successive bookings never collide on the same datetime.
  await expect(page.getByTestId('slot-picker')).toBeVisible();
  await page.locator('[data-testid^="slot-day-"]').first().click();
  await page.locator('[data-testid^="slot-time-"]:not([disabled])').nth(nextSlotIndex++).click();
  await page.getByTestId('consulta-proximo').click();

  // Step 4 — review → confirm → protocol (BR7).
  await expect(page.getByTestId('consulta-revisao')).toBeVisible();
  await page.getByTestId('consulta-confirmar').click();
  await expect(page.getByTestId('consulta-protocolo')).toBeVisible();
  const protocol = (await page.getByTestId('consulta-protocolo').textContent())?.trim() ?? '';
  expect(protocol).toMatch(/^AG-/);
  return protocol;
}

test('AC1: full consultation booking yields a protocol and shows in Próximos as AGENDADO', async ({ page }) => {
  await login(page);
  const protocol = await bookConsultation(page);

  await page.getByTestId('consulta-ver-agendamentos').click();
  await expect(page.getByTestId('meus-agendamentos-page')).toBeVisible();
  // Próximos is the default tab; the fresh booking is there as Agendado with its protocol.
  // Scope to the card <li>: inner nodes (meus-card-protocolo/-status/…) share the `meus-card-` prefix.
  const card = page.locator('li[data-testid^="meus-card-"]', { hasText: protocol });
  await expect(card).toBeVisible();
  await expect(card.getByTestId('meus-card-status')).toContainText('Agendado');
});

test('AC2/BR4: the exam wizard blocks advancing without the medical order attached', async ({ page }) => {
  await login(page);
  await page.getByTestId('nav-agendamento').click();
  await page.getByTestId('agendamento-hub-exame').click();
  await expect(page.getByTestId('exame-wizard-page')).toBeVisible();

  // Choose an exam and move to the mandatory-attachment step.
  await page.locator('[data-testid^="option-item-"]').first().click();
  await page.getByTestId('exame-proximo').click();
  await expect(page.getByTestId('exame-anexo')).toBeVisible();

  // Try to advance with no file — BR2 gate blocks it and shows a message; still on the attachment step.
  await page.getByTestId('exame-proximo').click();
  await expect(page.getByTestId('exame-gate')).toBeVisible();
  await expect(page.getByTestId('exame-anexo')).toBeVisible();
});

test('AC4/BR9: cancel moves the appointment to Histórico as CANCELADO, then rebook', async ({ page }) => {
  await login(page);
  const protocol = await bookConsultation(page);
  await page.getByTestId('consulta-ver-agendamentos').click();
  await expect(page.getByTestId('meus-agendamentos-page')).toBeVisible();

  // Cancel THIS test's own commitment (by its protocol, not `.first()` — the run's shared database
  // may hold other upcoming cards). Scope to that card <li>: the dialog (meus-cancelar-dialog/
  // -motivo/-confirmar) shares the `meus-cancelar-` prefix.
  await page
    .locator('li[data-testid^="meus-card-"]', { hasText: protocol })
    .locator('[data-testid^="meus-cancelar-"]')
    .click();
  // The p-dialog host (meus-cancelar-dialog) carries no visible box — its content renders in the
  // overlay (same as perfil.spec). Assert the confirm action instead, which is what appears.
  await expect(page.getByTestId('meus-cancelar-confirmar')).toBeVisible();
  await page.getByTestId('meus-cancelar-motivo').fill('Imprevisto');
  await page.getByTestId('meus-cancelar-confirmar').click();

  // It leaves Próximos and appears in Histórico as Cancelado (BR9/BR12).
  await page.getByTestId('meus-tab-historico').click();
  const cancelled = page.locator('li[data-testid^="meus-card-"]', { hasText: protocol });
  await expect(cancelled).toBeVisible();
  await expect(cancelled.getByTestId('meus-card-status')).toContainText('Cancelado');

  // Rebook a fresh consultation — the released seat is bookable again.
  const newProtocol = await bookConsultation(page);
  expect(newProtocol).not.toBe(protocol);
  await page.getByTestId('consulta-ver-agendamentos').click();
  await expect(
    page.locator('li[data-testid^="meus-card-"]', { hasText: newProtocol }),
  ).toBeVisible();
});
