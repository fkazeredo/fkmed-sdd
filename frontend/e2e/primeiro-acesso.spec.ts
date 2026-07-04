import { APIRequestContext, expect, test } from '@playwright/test';

/**
 * SPEC-0002 AC1 end to end over the isolated stack: PEDRO's first access (identity triple → account
 * creation) → the verification link is caught by Mailpit → confirming it activates the account →
 * PEDRO logs in and reaches an authenticated screen. Nothing is stubbed: real backend, real e-mail.
 */

const MAILPIT = 'http://localhost:8025';

/** Polls Mailpit for the newest message to {@code recipient} and returns its verification link. */
async function verificationLink(request: APIRequestContext, recipient: string): Promise<string> {
  let link = '';
  await expect
    .poll(
      async () => {
        const list = await request.get(`${MAILPIT}/api/v1/messages?limit=20`);
        const { messages } = (await list.json()) as { messages: { ID: string }[] };
        for (const summary of messages) {
          const detail = await request.get(`${MAILPIT}/api/v1/message/${summary.ID}`);
          const message = (await detail.json()) as {
            Text: string;
            To: { Address: string }[];
          };
          if (message.To.some((to) => to.Address === recipient)) {
            const match = message.Text.match(/https?:\/\/\S*\/verificar-email\?token=\S+/);
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

test('PEDRO first access → e-mail link → confirm → login reaches Meu Plano', async ({
  page,
  request,
}) => {
  // Step 1 — identity triple (PEDRO, the seeded dependent, now of age).
  await page.goto('/primeiro-acesso');
  await expect(page.getByRole('heading', { name: 'Primeiro acesso' })).toBeVisible();
  await page.getByLabel('CPF').fill('15350946056');
  await page.getByLabel('Número da carteirinha').fill('001234575');
  await page.getByLabel('Data de nascimento').fill('2007-05-20');
  await page.getByTestId('step1-submit').click();

  // Step 2 — create the account, exercising show/hide and the acceptance checkboxes.
  await expect(page.getByTestId('step2')).toBeVisible();
  await page.getByLabel('E-mail').fill('pedro@fkmed.local');
  await page.getByLabel('Senha').fill('Pedro1234');
  await page.getByTestId('toggle-password').click();
  await page.getByTestId('accept-terms').check();
  await page.getByTestId('accept-privacy').check();
  await page.getByTestId('step2-submit').click();

  // Step 3 — the "check your e-mail" notice.
  await expect(page.getByTestId('step3')).toBeVisible();

  // Fetch the verification link from Mailpit and confirm.
  const link = await verificationLink(request, 'pedro@fkmed.local');
  await page.goto(new URL(link).pathname + new URL(link).search);
  await expect(page.getByTestId('verify-confirmed')).toBeVisible();

  // Log in with the freshly activated account and land on an authenticated screen.
  await page.getByTestId('verify-login').click();
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
  await page.getByLabel('E-mail').fill('pedro@fkmed.local');
  await page.getByLabel('Senha').fill('Pedro1234');
  await page.getByRole('button', { name: 'Entrar' }).click();

  await expect(page.getByRole('heading', { name: 'Meu Plano' })).toBeVisible();
  const rows = page.getByTestId('member-row');
  await expect(rows).toHaveCount(2);
  await expect(rows.nth(1)).toContainText('PEDRO SOUZA LIMA');
});
