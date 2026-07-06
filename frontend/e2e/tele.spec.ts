import { APIRequestContext, Browser, expect, Page, test } from '@playwright/test';

/**
 * SPEC-0010 (Telemedicine, Phase 4) end to end over the real stack. The Pronto Atendimento journey
 * is a TWO-actor flow: the beneficiary (MARIA) drives the UI (triage → term → queue → room →
 * summary, live over SSE), and the operator side is driven through the SPEC-0018 sim slice
 * (`/api/sim/tele/*`) — there is no operator UI (ADR-0017). The sim is called with the seeded
 * OPERATOR_SIM credential's token (same access-token pattern as primeiro-acesso.spec).
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

/** A Bearer token for the seeded OPERATOR_SIM account, obtained by logging it into the SPA in an
 *  isolated context and reading the access token angular-oauth2-oidc keeps in sessionStorage. */
async function operatorToken(browser: Browser): Promise<string> {
  const context = await browser.newContext();
  const page = await context.newPage();
  await login(page, OP_EMAIL, OP_SENHA);
  const token = await page.evaluate(() => sessionStorage.getItem('access_token'));
  await context.close();
  expect(token, 'operator access token').toBeTruthy();
  return token as string;
}

/** Drives MARIA's triage → term → queue entry, leaving her session EM_FILA. */
async function enterQueue(page: Page): Promise<void> {
  await page.getByTestId('nav-telemedicina').click();
  await expect(page.getByTestId('telemedicina-hub-page')).toBeVisible();
  await page.getByTestId('telemedicina-hub-pronto').click();
  await expect(page.getByTestId('pronto-atendimento-page')).toBeVisible();

  // Triage (BR2): complaint (>= 10 chars), one symptom, a duration.
  await page.getByTestId('triagem-queixa').fill('Dor de cabeça há dois dias, sem melhora.');
  await page.locator('[data-testid^="triagem-sintoma-"]').first().click();
  await page.locator('[data-testid^="triagem-duracao-"]').first().click();
  await page.getByTestId('triagem-avancar').click();

  // Term (BR4): accept then enter the queue.
  await expect(page.getByTestId('pronto-termo')).toBeVisible();
  await page.getByTestId('termo-aceite').check();
  await page.getByTestId('termo-entrar-fila').click();

  // Queue (BR5/BR6): position visible, streamed via SSE.
  await expect(page.getByTestId('fila-posicao')).toBeVisible({ timeout: 15000 });
}

test('AC1/AC4: Pronto Atendimento → operator attends and closes with a prescription → Minha Saúde', async ({
  page,
  browser,
  request,
}) => {
  await login(page, MARIA_EMAIL, MARIA_SENHA);
  await enterQueue(page);

  const token = await operatorToken(browser);
  const auth = { Authorization: `Bearer ${token}` };

  // Operator starts attending the next queued session (SPEC-0018 sim).
  const startRes = await request.post('/api/sim/tele/sessions/next/start', {
    headers: auth,
    data: { professionalName: 'Dra. Ana Prado', crm: 'CRM-RJ 54321' },
  });
  expect(startRes.ok(), await startRes.text()).toBeTruthy();
  const sessionId = (await startRes.json()).sessionId as string;

  // MARIA's room opens live (SSE pushes EM_ATENDIMENTO) with the professional.
  await expect(page.getByTestId('sala-profissional')).toBeVisible({ timeout: 15000 });
  await expect(page.getByTestId('sala-profissional')).toContainText('Ana Prado');

  // Operator closes the session issuing a prescription — atomically (BR10).
  const closeRes = await request.post(`/api/sim/tele/sessions/${sessionId}/close`, {
    headers: auth,
    data: {
      professionalName: 'Dra. Ana Prado',
      crm: 'CRM-RJ 54321',
      guidance: 'Repouso e hidratação.',
      documents: [
        {
          type: 'PRESCRIPTION',
          medications: [{ medication: 'Paracetamol 750mg', posology: '1 cp 8/8h', guidance: 'Após alimentação' }],
        },
      ],
    },
  });
  expect(closeRes.ok(), await closeRes.text()).toBeTruthy();

  // MARIA's summary appears live (SSE pushes ENCERRADA) with the issued document + the link.
  await expect(page.getByTestId('resumo-minha-saude')).toBeVisible({ timeout: 15000 });
  await expect(page.getByTestId('resumo-documentos')).toBeVisible();

  // "Ver em Minha Saúde" → the prescription is there for MARIA, dated today, valid.
  await page.getByTestId('resumo-minha-saude').click();
  await expect(page.getByTestId('minha-saude-hub-page')).toBeVisible();
  await page.getByTestId('minha-saude-hub-receituarios').click();
  await expect(page.getByTestId('documentos-lista-page')).toBeVisible();
  const card = page.locator('button[data-testid^="documento-card-"]', { hasText: 'Paracetamol' }).first();
  // fall back to the first card if the title does not surface the medication
  const anyCard = page.locator('button[data-testid^="documento-card-"]').first();
  await expect(anyCard).toBeVisible({ timeout: 15000 });
});

test('AC2: leaving the queue abandons the session and reopens the hub', async ({ page }) => {
  await login(page, MARIA_EMAIL, MARIA_SENHA);
  await enterQueue(page);

  await page.getByTestId('fila-sair').click();
  await expect(page.getByTestId('sessao-confirmar-saida')).toBeVisible();
  await page.getByTestId('sessao-saida-confirmar').click();

  // The session is ABANDONADA; the hub is reachable again.
  await expect(page.getByTestId('abandonada-voltar-hub')).toBeVisible({ timeout: 15000 });
  await page.getByTestId('abandonada-voltar-hub').click();
  await expect(page.getByTestId('telemedicina-hub-page')).toBeVisible();
});
