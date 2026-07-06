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

  // Step 3 — date/time; pick the first day then the first OPEN slot (full ones are disabled, BR5).
  await expect(page.getByTestId('slot-picker')).toBeVisible();
  await page.locator('[data-testid^="slot-day-"]').first().click();
  await page.locator('[data-testid^="slot-time-"]:not([disabled])').first().click();
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
  const card = page.locator('[data-testid^="meus-card-"]', { hasText: protocol });
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

  // Cancel the just-booked commitment (until start, BR9).
  await page.locator('[data-testid^="meus-cancelar-"]').first().click();
  await expect(page.getByTestId('meus-cancelar-dialog')).toBeVisible();
  await page.getByTestId('meus-cancelar-motivo').fill('Imprevisto');
  await page.getByTestId('meus-cancelar-confirmar').click();

  // It leaves Próximos and appears in Histórico as Cancelado (BR9/BR12).
  await page.getByTestId('meus-tab-historico').click();
  const cancelled = page.locator('[data-testid^="meus-card-"]', { hasText: protocol });
  await expect(cancelled).toBeVisible();
  await expect(cancelled.getByTestId('meus-card-status')).toContainText('Cancelado');

  // Rebook a fresh consultation — the released seat is bookable again.
  const newProtocol = await bookConsultation(page);
  expect(newProtocol).not.toBe(protocol);
  await page.getByTestId('consulta-ver-agendamentos').click();
  await expect(
    page.locator('[data-testid^="meus-card-"]', { hasText: newProtocol }),
  ).toBeVisible();
});
