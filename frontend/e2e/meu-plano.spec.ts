import { expect, test } from '@playwright/test';

/**
 * SPEC-0001 AC3 — the walking-skeleton journey, end to end over the real stack:
 * dev login (SPEC-0002 seam) → OIDC Code+PKCE → Meu Plano shows the plan name, ANS 326305,
 * coverage, additive and both family members served by the API from the seeded database.
 */
test('dev login → Meu Plano shows the seeded plan and family', async ({ page }) => {
  await page.goto('/');

  // Unauthenticated: the guard sends the browser to the AS pt-BR login page.
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();

  await page.getByLabel('Usuário').fill('maria');
  await page.getByLabel('Senha').fill('dev12345');
  await page.getByRole('button', { name: 'Entrar' }).click();

  // Back in the SPA, authenticated: shell + Meu Plano.
  await expect(page.getByTestId('brand')).toHaveText('FKMed');
  await expect(page.getByRole('heading', { name: 'Meu Plano' })).toBeVisible();

  // Plan data (BR4) — served by the API from the V1 seed, nothing hardcoded (BR6).
  await expect(page.getByTestId('plan-name')).toHaveText(
    'PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP',
  );
  await expect(page.getByTestId('plan-ans')).toHaveText('326305');
  await expect(page.getByTestId('plan-coverage')).toHaveText('Estadual');
  await expect(page.getByTestId('plan-copay')).toContainText('Sim');
  await expect(page.getByTestId('plan-reimbursement')).toContainText('Sim');
  await expect(page.getByTestId('plan-additive')).toHaveText(
    'Urg/emerg Nacional Hr — Assistência',
  );

  // Family members (BR5) with card numbers.
  const rows = page.getByTestId('member-row');
  await expect(rows).toHaveCount(2);
  await expect(rows.nth(0)).toContainText('MARIA CLARA SOUZA LIMA');
  await expect(rows.nth(0)).toContainText('Titular');
  await expect(rows.nth(0)).toContainText('001234567');
  await expect(rows.nth(1)).toContainText('PEDRO SOUZA LIMA');
  await expect(rows.nth(1)).toContainText('Dependente');
  await expect(rows.nth(1)).toContainText('001234575');
});

test('health is UP through the frontend proxy (BR1)', async ({ request }) => {
  const response = await request.get('/api/system/health');
  expect(response.status()).toBe(200);
  expect(await response.json()).toMatchObject({ status: 'UP', database: 'UP' });
});

test('the API refuses unauthenticated calls with 401 (BR3/AC2)', async ({ request }) => {
  const response = await request.get('/api/plan/my-plan');
  expect(response.status()).toBe(401);
});
