import { expect, test } from '@playwright/test';

/**
 * SPEC-0006 Perfil & Conta journeys over the isolated stack (real backend). These author the
 * three critical flows; the architect runs them at integration (my gate is lint + unit + build).
 *
 * Seed expectations (backend, isolated stack — to confirm at integration):
 *  - `perfil-e2e@fkmed.local` / `perfilE2e12345`: a disposable TITULAR account whose contact data
 *    may be freely mutated by this file (mirrors the disposable account pattern of
 *    `seguranca-conta.spec.ts`), with both legal documents already accepted so it is NOT
 *    intercepted.
 *  - `termos-e2e@fkmed.local` / `termosE2e12345`: an account for which a NEW mandatory Terms
 *    version has been published and NOT yet accepted, so login is intercepted (BR8).
 * `workers: 1` already applies (playwright.config.ts): the stack seeds once per compose up.
 */

const PERFIL_EMAIL = 'perfil-e2e@fkmed.local';
const PERFIL_PASSWORD = 'perfilE2e12345';
const TERMOS_EMAIL = 'termos-e2e@fkmed.local';
const TERMOS_PASSWORD = 'termosE2e12345';

async function login(page: import('@playwright/test').Page, email: string, password: string): Promise<void> {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
}

test('Alterar Cadastro: edita o celular, salva parcialmente e o novo valor persiste ao reabrir (AC2)', async ({
  page,
}) => {
  await login(page, PERFIL_EMAIL, PERFIL_PASSWORD);
  await expect(page.getByTestId('home-page')).toBeVisible();

  await page.getByTestId('nav-perfil').click();
  await expect(page.getByTestId('perfil-menu-page')).toBeVisible();
  await page.getByTestId('perfil-item-alterar-cadastro').click();
  await expect(page.getByTestId('alterar-cadastro-page')).toBeVisible();

  // Contract data is read-only with the service-channel hint (BR4).
  await expect(page.getByTestId('cadastro-contrato-hint')).toContainText('canais de atendimento');

  const novoCelular = '(21) 98888-7766';
  await page.getByLabel('Celular').fill(novoCelular);
  await page.getByTestId('cadastro-submit').click();
  await expect(page.getByTestId('cadastro-success')).toBeVisible();

  // Reopen: the new value is there (persisted).
  await page.reload();
  await page.getByTestId('nav-perfil').click();
  await page.getByTestId('perfil-item-alterar-cadastro').click();
  await expect(page.getByLabel('Celular')).toHaveValue(novoCelular);
});

test('Alterar Cadastro: celular vazio bloqueia o salvamento com a mensagem obrigatória (AC7, BR6)', async ({
  page,
}) => {
  await login(page, PERFIL_EMAIL, PERFIL_PASSWORD);
  await page.getByTestId('nav-perfil').click();
  await page.getByTestId('perfil-item-alterar-cadastro').click();
  await expect(page.getByTestId('alterar-cadastro-page')).toBeVisible();

  await page.getByLabel('Celular').fill('');
  await expect(page.getByTestId('error-mobile')).toBeVisible();
  await expect(page.getByTestId('cadastro-submit')).toBeDisabled();
});

test('Interceptação de termos: nova versão bloqueia a navegação até "Li e aceito" (AC5, BR8)', async ({
  page,
}) => {
  await login(page, TERMOS_EMAIL, TERMOS_PASSWORD);

  // A new mandatory version is unaccepted → intercepted before any internal screen.
  await expect(page.getByTestId('legal-acceptance-page')).toBeVisible();
  await expect(page.getByTestId('aceite-TERMS')).toBeVisible();

  // Every other internal route stays blocked: trying the nav bounces back to the acceptance screen.
  await page.getByTestId('nav-home').click();
  await expect(page.getByTestId('legal-acceptance-page')).toBeVisible();
  await expect(page.getByTestId('home-page')).toHaveCount(0);

  // Accept → navigation resumes and Home renders.
  await page.getByTestId('aceite-botao-TERMS').click();
  await expect(page.getByTestId('home-page')).toBeVisible();
});

test('Sair pelo menu Perfil: confirmação encerra a sessão e exige novo login (AC6, BR9)', async ({ page }) => {
  await login(page, PERFIL_EMAIL, PERFIL_PASSWORD);
  await expect(page.getByTestId('home-page')).toBeVisible();

  await page.getByTestId('nav-perfil').click();
  await page.getByTestId('perfil-item-sair').click();
  // The confirmation is a PrimeNG p-dialog whose host element (data-testid="perfil-sair-dialog")
  // carries no visible box — the dialog content renders in the overlay. Assert the dialog's actual
  // content (its message + confirm action), which is what appears on screen (BR9).
  await expect(page.getByTestId('perfil-sair-mensagem')).toBeVisible();
  await expect(page.getByTestId('perfil-sair-confirmar')).toBeVisible();
  await page.getByTestId('perfil-sair-confirmar').click();

  // Session ended: reaching an internal screen requires logging in again.
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
});
