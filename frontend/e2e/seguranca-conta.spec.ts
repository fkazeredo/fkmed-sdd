import { APIRequestContext, expect, test } from '@playwright/test';

/**
 * SPEC-0002 account-security journeys over the isolated stack (real backend, real e-mail via
 * Mailpit): forgot/reset password (BR10), authenticated password change (BR11) and the lockout
 * message (BR8). All three tests mutate and eventually lock a disposable account seeded
 * specifically for this file — `seguranca-e2e@fkmed.local` (its own family, one titular, no
 * dependents) — and MUST run in this file's declaration order (Playwright preserves in-file
 * order): each depends on the password left by the previous one, and the last test deliberately
 * locks the account for 15 real minutes. MARIA's canonical account (used by `meu-plano.spec.ts`
 * and `primeiro-acesso.spec.ts`) is never touched by this file — there is no longer a
 * file-naming/ordering coupling to it (previously this file had to sort after `meu-plano.spec.ts`
 * to avoid racing MARIA's login; that hack is gone now that the account under test here is
 * disposable). `workers: 1` still applies (see playwright.config.ts): the isolated stack seeds its
 * data once per `docker compose up`, not once per spec file, so this file's own account-locking
 * test would otherwise race any other spec run concurrently — not because of MARIA anymore, but
 * because the disposable account itself must not be touched by two spec files at once.
 */

const MAILPIT = 'http://localhost:8025';
const SEGURANCA_EMAIL = 'seguranca-e2e@fkmed.local';
const ORIGINAL_PASSWORD = 'seguranca12345';
const RESET_PASSWORD = 'SegE2eReset01';
const CHANGED_PASSWORD = 'SegE2eTroca02';

/** Polls Mailpit for the newest message to {@code recipient} and returns its reset-password link. */
async function resetLink(request: APIRequestContext, recipient: string): Promise<string> {
  let link = '';
  await expect
    .poll(
      async () => {
        const list = await request.get(`${MAILPIT}/api/v1/messages?limit=20`);
        const { messages } = (await list.json()) as { messages: { ID: string }[] };
        for (const summary of messages) {
          const detail = await request.get(`${MAILPIT}/api/v1/message/${summary.ID}`);
          const message = (await detail.json()) as { Text: string; To: { Address: string }[] };
          if (message.To.some((to) => to.Address === recipient)) {
            const match = message.Text.match(/https?:\/\/\S*\/redefinir-senha\?token=\S+/);
            if (match) {
              link = match[0].replace(/[)>.,\s]+$/, '');
              return true;
            }
          }
        }
        return false;
      },
      { timeout: 20_000, intervals: [500, 1000, 2000] },
    )
    .toBe(true);
  return link;
}

test('esqueci minha senha → Mailpit → redefinir senha → nova senha funciona, antiga não (BR10)', async ({
  page,
  request,
}) => {
  await page.goto('/recuperar-senha');
  await expect(page.getByRole('heading', { name: 'Esqueci minha senha' })).toBeVisible();
  await page.getByLabel('E-mail').fill(SEGURANCA_EMAIL);
  await page.getByTestId('forgot-password-submit').click();
  await expect(page.getByTestId('forgot-password-done')).toBeVisible();

  const link = await resetLink(request, SEGURANCA_EMAIL);
  await page.goto(new URL(link).pathname + new URL(link).search);

  await expect(page.getByRole('heading', { name: 'Redefinir senha' })).toBeVisible();
  await page.getByLabel('Nova senha', { exact: true }).fill(RESET_PASSWORD);
  await page.getByLabel('Confirme a nova senha').fill(RESET_PASSWORD);
  await page.getByTestId('reset-password-submit').click();
  await expect(page.getByTestId('reset-password-success')).toBeVisible();

  // The new password works.
  await page.getByTestId('reset-password-login').click();
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
  await page.getByLabel('E-mail').fill(SEGURANCA_EMAIL);
  await page.getByLabel('Senha').fill(RESET_PASSWORD);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('heading', { name: 'Meu Plano' })).toBeVisible();

  // The old password no longer works (BR10: reset replaces the credential outright).
  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
  await page.getByLabel('E-mail').fill(SEGURANCA_EMAIL);
  await page.getByLabel('Senha').fill(ORIGINAL_PASSWORD);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByText('Dados de acesso inválidos.')).toBeVisible();
});

test('Segurança: troca de senha autenticada → novo login funciona (BR11, AC-8)', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
  await page.getByLabel('E-mail').fill(SEGURANCA_EMAIL);
  await page.getByLabel('Senha').fill(RESET_PASSWORD);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('heading', { name: 'Meu Plano' })).toBeVisible();

  await page.getByTestId('nav-seguranca').click();
  await expect(page.getByRole('heading', { name: 'Segurança' })).toBeVisible();
  await expect(page.getByTestId('security-email')).toHaveText(SEGURANCA_EMAIL);

  await page.getByLabel('Senha atual').fill(RESET_PASSWORD);
  await page.getByLabel('Nova senha', { exact: true }).fill(CHANGED_PASSWORD);
  await page.getByLabel('Confirme a nova senha').fill(CHANGED_PASSWORD);
  await page.getByTestId('security-submit').click();
  await expect(page.getByTestId('security-success')).toBeVisible();

  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
  await page.getByLabel('E-mail').fill(SEGURANCA_EMAIL);
  await page.getByLabel('Senha').fill(CHANGED_PASSWORD);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByRole('heading', { name: 'Meu Plano' })).toBeVisible();

  // Leaves unauthenticated for the next (lockout) test.
  await page.getByRole('button', { name: 'Sair' }).click();
});

test('bloqueio após 5 tentativas erradas exibe a mensagem de bloqueio (BR8)', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();

  for (let attempt = 0; attempt < 5; attempt++) {
    await page.getByLabel('E-mail').fill(SEGURANCA_EMAIL);
    await page.getByLabel('Senha').fill('senha-errada-de-proposito');
    await page.getByRole('button', { name: 'Entrar' }).click();
    await expect(page.getByText('Dados de acesso inválidos.')).toBeVisible();
  }

  // 6th attempt, even with a different (irrelevant) password: the account is now locked
  // (BR8/DL-0002) — the UI shows the lock state instead of the generic invalid-credentials one.
  await page.getByLabel('E-mail').fill(SEGURANCA_EMAIL);
  await page.getByLabel('Senha').fill(CHANGED_PASSWORD);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(
    page.getByText(
      'Sua conta está temporariamente bloqueada após várias tentativas. Tente novamente em alguns minutos.',
    ),
  ).toBeVisible();
});
