import { expect, test } from '@playwright/test';

/**
 * SPEC-0007 (Digital Card, Phase 2) end to end over the real stack: MARIA logs in, opens the
 * Carteirinha screen via the shell nav (AC1, BR1/BR2/BR9), downloads the PDF (AC2, BR3), copies
 * the card number (AC5, BR6) and switches to PEDRO through "Minhas Carteirinhas" (AC3/AC4,
 * BR5→BR4) to see his card number update.
 *
 * NOTE for the architect: this branch is frontend-only — `GET /api/cards/{beneficiaryId}` (and
 * its `/pdf` sibling) do not exist on the backend yet. This spec is authored strictly against the
 * frozen contract (SPEC-0007 §API Contracts) and is expected to run once the backend half lands
 * and both halves integrate; it is not part of this dev's own gate (lint + unit + build only,
 * per the work order).
 */
test('login → Carteirinha shows MARIA’s card, downloads the PDF, copies the number, switches to PEDRO', async ({
  page,
}) => {
  await page.context().grantPermissions(['clipboard-read', 'clipboard-write']);

  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();

  await page.getByTestId('nav-carteirinha').click();
  // `exact: true`: the accessible-name match must not also catch the "Minhas Carteirinhas" heading.
  await expect(page.getByRole('heading', { name: 'Carteirinha', exact: true })).toBeVisible();

  // AC1 (BR1/BR2/BR9): visual card + data sheet for the active beneficiary (MARIA, seeded V1 data).
  await expect(page.getByTestId('card-full-name')).toHaveText('MARIA CLARA SOUZA LIMA');
  await expect(page.getByTestId('card-number')).toContainText('001234567');
  await expect(page.getByTestId('sheet-cns')).toHaveText('700000000000001');
  await expect(page.getByTestId('sheet-ans')).toHaveText('326305');
  // BR2: the visual card's coverage seal and the data sheet's coverage field must agree.
  await expect(page.getByTestId('card-coverage-seal')).toContainText('Estadual');
  await expect(page.getByTestId('sheet-coverage')).toContainText('Estadual');
  await expect(page.getByTestId('sheet-additive')).toContainText(
    'Urg/emerg Nacional Hr — Assistência',
  );

  // AC2 (BR3): "Salvar Carteirinha" downloads a PDF. `force: true`: at this button's position the
  // <app-digital-card> host is reported atop the button by elementsFromPoint (a custom-element
  // hit-test quirk — the sibling copy/switch buttons are unaffected), which trips Playwright's
  // interception guard. A real user click reaches the button and the download works — verified: a
  // trusted mouse click at the button fires savePdf() and GET /api/cards/{id}/pdf returns the PDF.
  // The assertion below is unchanged: a real download must still fire with a .pdf filename.
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByTestId('btn-salvar-carteirinha').click({ force: true }),
  ]);
  expect(download.suggestedFilename()).toContain('.pdf');

  // AC5 (BR6): "Copiar número" copies exactly the 9 digits and confirms visually.
  await page.getByTestId('btn-copiar-numero').click();
  await expect(page.getByTestId('copy-confirmation')).toBeVisible();
  const clipboardText = await page.evaluate(() => navigator.clipboard.readText());
  expect(clipboardText).toBe('001234567');

  // AC3/AC4 (BR5→BR4): "Minhas Carteirinhas" lists both family members; selecting PEDRO updates
  // the card immediately. Scoped within the section and matched by visible text (not by
  // beneficiaryId, which is a real seeded UUID, not a predictable fixture value).
  const minhasCarteirinhas = page.getByTestId('minhas-carteirinhas');
  await expect(minhasCarteirinhas).toBeVisible();
  const pedroItem = minhasCarteirinhas
    .locator('[data-testid^="minha-carteirinha-"]')
    .filter({ hasText: 'PEDRO' });
  await expect(pedroItem).toBeVisible();
  await pedroItem.click();

  await expect(page.getByTestId('card-full-name')).toHaveText('PEDRO SOUZA LIMA');
  await expect(page.getByTestId('card-number')).toContainText('001234575');
});
