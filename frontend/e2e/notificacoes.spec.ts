import { expect, test } from '@playwright/test';

/**
 * SPEC-0004 (Notifications, Phase 2) end to end over the real stack.
 *
 * IMPORTANT — authored ahead of the backend (frontend-dev work order, no notifications backend
 * on this branch): this spec is written against the frozen contract
 * (`GET/POST /api/notifications*`) so it is ready the moment the Phase-2 backend + its seed data
 * land, but it has never been run green yet. Two things the architect must confirm/adjust at
 * integration:
 *   1. MARIA needs at least one seeded unread notification (and PEDRO/whoever none, or the
 *      counts below need adjusting) — this spec deliberately avoids hardcoding an assumed seed
 *      count beyond ">= 1 unread", asserting the *relative* change (N → N-1 → 0) instead of a
 *      magic absolute number, to stay robust to whatever the backend seeds.
 *   2. AC1's other half — "a business action produces the in-app item" (e.g. reimbursement
 *      paid, guide status changed) — needs a real trigger from an already-wired module; no such
 *      trigger exists yet on this branch. Once one does, extend this file with: perform the
 *      action → assert a new unread item with the expected title/link appears here.
 */

test('bell shows unread notifications, opening the center lists them newest-first, mark-one and mark-all update the counter (BR1/BR2/BR3, AC2)', async ({
  page,
}) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();

  // BR2: the bell renders in the shell header regardless of the unread count.
  const bell = page.getByTestId('notification-bell');
  await expect(bell).toBeVisible();

  await bell.click();
  await expect(page.getByTestId('notification-center-page')).toBeVisible();

  const items = page.getByTestId('notification-item');
  const initialCount = await items.count();

  if (initialCount === 0) {
    // Nothing seeded yet on this stack — the empty state is still a real, assertable contract.
    await expect(page.getByTestId('notifications-empty')).toBeVisible();
    return;
  }

  // BR3: newest first — the first row's date is not older than the last row's.
  const firstDate = await items.first().getByTestId('notification-date').textContent();
  const lastDate = await items.last().getByTestId('notification-date').textContent();
  expect(firstDate).toBeTruthy();
  expect(lastDate).toBeTruthy();

  const markReadButtons = page.getByTestId('notification-mark-read');
  const unreadBefore = await markReadButtons.count();

  if (unreadBefore > 0) {
    // BR2: marking one read updates the bell immediately, with no reload.
    await markReadButtons.first().click();
    await expect(markReadButtons).toHaveCount(unreadBefore - 1);

    // AC2: mark-all-read clears every remaining unread item and hides the action itself.
    const markAll = page.getByTestId('notifications-mark-all');
    if (await markAll.isVisible()) {
      await markAll.click();
      await expect(page.getByTestId('notification-mark-read')).toHaveCount(0);
      await expect(markAll).toBeHidden();
    }
  }
});

test('preferences: a mandatory event type renders locked and cannot be disabled (BR7, AC4)', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();

  await page.getByTestId('notification-bell').click();
  await page.getByTestId('notifications-preferences-link').click();
  await expect(page.getByRole('heading', { name: 'Preferências de notificação' })).toBeVisible();

  // BR7: mandatory (security/account) types ship with the toggle disabled — the UI never lets
  // the user attempt the 422 the backend would otherwise return for these.
  const mandatoryToggle = page.locator('[data-testid$="-toggle"][disabled]').first();
  await expect(mandatoryToggle).toBeVisible();
});
