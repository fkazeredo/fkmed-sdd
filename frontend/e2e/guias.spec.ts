import { Browser, expect, Page, test } from '@playwright/test';

/**
 * SPEC-0012 (Guides and Tokens, Phase 5) end to end over the real stack. The guide journey is the
 * phase driver: the beneficiary (MARIA) only follows guides, and the operator side is driven through
 * the SPEC-0018 sim (`/api/sim/guides/*`) — there is no operator UI (ADR-0017). The sim is called
 * with the seeded OPERATOR_SIM credential's token (same access-token pattern as tele.spec).
 *
 * To stay self-contained and retry-safe (workers:1, one shared live Postgres per `docker compose
 * up`), the AC7 test CREATES its own EM_ANALISE guide through the sim and authorizes THAT one,
 * instead of mutating a seeded guide (which a retry would then find already authorized).
 */

const MARIA_EMAIL = 'maria@fkmed.local';
const MARIA_SENHA = 'maria12345';
const OP_EMAIL = 'operador-sim@fkmed.local';
const OP_SENHA = 'operador12345';

async function login(page: Page, email: string, senha: string): Promise<void> {
  await page.goto('/');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(senha);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();
}

/** A Bearer token for the seeded OPERATOR_SIM account (see tele.spec for the rationale — this
 *  internal credential is routed to /aceite-legal, never Home, so we wait for the stored token). */
async function operatorToken(browser: Browser): Promise<string> {
  const context = await browser.newContext();
  const page = await context.newPage();
  await page.goto('/');
  await page.getByLabel('E-mail').fill(OP_EMAIL);
  await page.getByLabel('Senha').fill(OP_SENHA);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await page.waitForFunction(() => !!sessionStorage.getItem('access_token'), null, { timeout: 15000 });
  const token = await page.evaluate(() => sessionStorage.getItem('access_token'));
  await context.close();
  expect(token, 'operator access token').toBeTruthy();
  return token as string;
}

async function openGuias(page: Page): Promise<void> {
  await page.goto('/guias');
  await expect(page.getByTestId('guias-page')).toBeVisible();
}

/** The active beneficiary's id, read off the picker button whose label matches `firstName`. */
async function beneficiaryId(page: Page, firstName: RegExp): Promise<string> {
  await expect(page.getByTestId('guias-beneficiarios')).toBeVisible();
  const btn = page.locator('[data-testid^="guias-beneficiario-"]', { hasText: firstName }).first();
  const testId = await btn.getAttribute('data-testid');
  expect(testId, `beneficiary button for ${firstName}`).toBeTruthy();
  return (testId as string).replace('guias-beneficiario-', '');
}

test('AC7: operator authorizes a guide via the sim → status flips and the detail shows the password', async ({
  page,
  browser,
  request,
}) => {
  await login(page, MARIA_EMAIL, MARIA_SENHA);
  await openGuias(page);
  const mariaId = await beneficiaryId(page, /maria/i);

  const token = await operatorToken(browser);
  const auth = { Authorization: `Bearer ${token}` };

  // Operator opens a guide under analysis for MARIA (sim), then authorizes it (SPEC-0018 BR5).
  const createRes = await request.post('/api/sim/guides', {
    headers: auth,
    data: {
      beneficiaryId: mariaId,
      type: 'CONSULTA',
      requestingProvider: 'Clínica E2E',
      items: [{ tussCode: '10101012', description: 'Consulta médica', quantity: 1 }],
    },
  });
  expect(createRes.ok(), await createRes.text()).toBeTruthy();
  const guideId = (await createRes.json()).id as string;

  const authRes = await request.post(`/api/sim/guides/${guideId}/authorize`, {
    headers: auth,
    data: { password: 'AUT-777001', validUntil: '2026-12-31' },
  });
  expect(authRes.ok(), await authRes.text()).toBeTruthy();

  // MARIA refreshes (BR4): the new guide is AUTORIZADA in her list, and its detail shows the
  // authorization password + validity (AC7/AC3) — indistinguishable from a real back office.
  await page.getByTestId('guias-atualizar').click();
  const card = page.getByTestId(`guia-card-${guideId}`);
  await expect(card).toBeVisible({ timeout: 15000 });
  await expect(card.getByTestId('guia-card-status')).toContainText(/autorizada/i);

  await card.click();
  await expect(page.getByTestId('guia-detalhe-page')).toBeVisible();
  await expect(page.getByTestId('guia-detalhe-senha')).toContainText('AUT-777001');
  await expect(page.getByTestId('guia-detalhe-validade')).toBeVisible();
});

test('AC4/AC5: generate a token (6 digits + countdown), copy it, regenerate invalidates the previous', async ({
  page,
}) => {
  await page.context().grantPermissions(['clipboard-read', 'clipboard-write']);
  await login(page, MARIA_EMAIL, MARIA_SENHA);
  await openGuias(page);

  // Generate a token regardless of the starting state: the shared stack may already hold an active
  // token for MARIA from an earlier run (workers:1, one live Postgres per `up`), so the section can
  // be in `token-vazio` (button "Gerar") OR `token-ativo`/`token-expirado` (button "Gerar novo").
  await page.locator('[data-testid="token-gerar"], [data-testid="token-gerar-novo"]').first().click();
  await expect(page.getByTestId('token-codigo')).toBeVisible({ timeout: 15000 });
  const code1 = (await page.getByTestId('token-codigo').textContent())?.trim() ?? '';
  expect(code1).toMatch(/^\d{6}$/); // exactly 6 digits (BR9)
  await expect(page.getByTestId('token-countdown')).toBeVisible(); // 10:00 countdown (BR9)

  // Copy places exactly the code on the clipboard, with a visible confirmation (BR11).
  await page.getByTestId('token-copiar').click();
  await expect(page.getByTestId('token-copiado')).toBeVisible();
  const clip = await page.evaluate(() => navigator.clipboard.readText());
  expect(clip).toBe(code1);

  // Regenerate: the previous stops being valid and only a new code shows (BR9/AC5).
  await page.getByTestId('token-gerar-novo').click();
  await expect(page.getByTestId('token-codigo')).toBeVisible();
  await expect(async () => {
    const code2 = (await page.getByTestId('token-codigo').textContent())?.trim() ?? '';
    expect(code2).toMatch(/^\d{6}$/);
    expect(code2).not.toBe(code1);
  }).toPass({ timeout: 10000 });
});

test('AC2: a dependent with no guides sees the orientative empty state', async ({ page }) => {
  await login(page, MARIA_EMAIL, MARIA_SENHA);
  await openGuias(page);

  // Switch the active beneficiary to PEDRO (dependent, no guides in the seed) — the list reloads.
  const pedro = page.locator('[data-testid^="guias-beneficiario-"]', { hasText: /pedro/i }).first();
  await pedro.click();

  await expect(page.getByTestId('guias-vazio')).toBeVisible({ timeout: 15000 });
  await expect(page.getByTestId('guias-vazio-atualizar')).toBeVisible();
});
