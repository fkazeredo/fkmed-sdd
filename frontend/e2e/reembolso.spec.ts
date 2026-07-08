import { expect, Page, test } from '@playwright/test';

const MARIA_EMAIL = 'maria@fkmed.local';
const MARIA_SENHA = 'maria12345';
const NO_REIMBURSEMENT_EMAIL = 'reembolso-sem-direito-e2e@fkmed.local';
const NO_REIMBURSEMENT_SENHA = 'reembolso12345';

async function login(page: Page, email: string, senha: string): Promise<void> {
  await page.goto('/');
  await page.getByLabel('E-mail').fill(email);
  await page.getByLabel('Senha').fill(senha);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();
}

test('MARIA: reimbursement history, paid statement and immediate preview', async ({ page }) => {
  await login(page, MARIA_EMAIL, MARIA_SENHA);

  await page.getByTestId('nav-reembolso').click();
  await expect(page.getByTestId('reembolso-hub-page')).toBeVisible();

  await page.getByTestId('reembolso-tab-historico').click();
  await expect(page.getByTestId('reembolso-historico')).toContainText('RE-20260601-0001', {
    timeout: 15000,
  });
  await page.getByRole('button', { name: /RE-20260601-0001/ }).click();
  await expect(page.getByTestId('reembolso-detalhe')).toContainText('Pago');
  await expect(page.getByTestId('reembolso-detalhe')).toContainText('R$ 150,00');
  await expect(page.getByTestId('reembolso-detalhe')).toContainText('R$ 120,00');
  await expect(page.getByTestId('reembolso-detalhe')).toContainText('R$ 30,00');
  await expect(page.getByTestId('reembolso-detalhe')).toContainText('Valor excede a tabela do plano');

  await page.getByTestId('reembolso-tab-extrato').click();
  await expect(page.getByTestId('reembolso-extrato')).toContainText('RE-20260601-0001');
  await expect(page.getByTestId('reembolso-extrato')).toContainText('Total: R$ 120,00');

  await page.getByTestId('reembolso-tab-previas').click();
  await page.getByTestId('reembolso-previa-submit').click();
  await expect(page.getByTestId('reembolso-previa-resultado')).toBeVisible({ timeout: 15000 });
  await expect(page.getByTestId('reembolso-previa-resultado')).toContainText(/^PV-\d{8}-\d{4}/);
  await expect(page.getByTestId('reembolso-previa-resultado')).toContainText('R$ 120,00');
  await expect(page.getByTestId('reembolso-previa-resultado')).toContainText(
    'Não representa autorização nem garantia de pagamento',
  );
});

test('plan without reimbursement shows the informative gate', async ({ page }) => {
  await login(page, NO_REIMBURSEMENT_EMAIL, NO_REIMBURSEMENT_SENHA);

  await page.goto('/reembolso');
  await expect(page.getByTestId('reembolso-sem-direito')).toBeVisible({ timeout: 15000 });
  await expect(page.getByTestId('reembolso-sem-direito')).toContainText(/Seu plano n.o possui reembolso/);
});
