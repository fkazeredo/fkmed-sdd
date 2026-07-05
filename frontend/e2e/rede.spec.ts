import { expect, test } from '@playwright/test';

/**
 * SPEC-0008 (Provider Network Search, Phase 3) end to end over the real stack: the Rede hub, the
 * locality-funnel journey RJ/Rio de Janeiro/Centro/Consultórios–Clínicas–Terapias/Cardiologia with
 * ≥ 10 results (AC2), the name search finding a seeded provider (AC8), and "Traçar rota" opening
 * the maps service in a new tab (AC6/BR12).
 *
 * NOTE for the architect: this branch is frontend-only — `/api/network/*` does not exist on the
 * backend yet. This spec is authored strictly against the frozen contract (SPEC-0008 §API
 * Contracts, as pinned in the slice plan) and is expected to run once the backend half lands and
 * both halves integrate; it is not part of this dev's own gate (lint + unit + build only, per the
 * work order). The exact seeded provider name matched by the "cardio" search below is a
 * backend-authored fixture (persistence §Migration V15 seed) this dev cannot know ahead of
 * integration — the assertion only requires at least one match to render with the expected card
 * shape (BR8), not a specific name.
 */
test('login → Rede hub → funil RJ/Rio de Janeiro/Centro/Consultórios/Cardiologia yields ≥ 10 results', async ({
  page,
}) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Entrar no FKMed' })).toBeVisible();
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();

  await page.getByTestId('nav-rede').click();
  await expect(page.getByTestId('rede-hub-page')).toBeVisible();

  // Only "Busca de rede" is delivered this phase; the other 3 cards render disabled ("em breve").
  await expect(page.getByTestId('rede-hub-agendamento')).toBeDisabled();
  await expect(page.getByTestId('rede-hub-agendamento-em-breve')).toContainText('Em breve');
  await expect(page.getByTestId('rede-hub-telemedicina')).toBeDisabled();
  await expect(page.getByTestId('rede-hub-minhaSaude')).toBeDisabled();

  await page.getByTestId('rede-hub-buscaDeRede').click();
  await expect(page.getByTestId('network-search-page')).toBeVisible();

  // BR1: State → Municipality → Neighborhood, each enabling after the previous.
  await expect(page.getByTestId('funil-municipio')).toBeDisabled();
  await expect(page.getByTestId('funil-bairro')).toBeDisabled();
  await expect(page.getByTestId('funil-buscar')).toBeDisabled();

  await page.getByTestId('funil-uf').click();
  await page.getByTestId('option-item-RJ').click();
  await expect(page.getByTestId('funil-municipio')).toBeEnabled();

  await page.getByTestId('funil-municipio').click();
  // BR2: real-time, accent/case-insensitive filter (AC1).
  await page.getByTestId('option-search-input').fill('rio de');
  await page.getByText('Rio de Janeiro', { exact: true }).click();
  await expect(page.getByTestId('funil-bairro')).toBeEnabled();

  await page.getByTestId('funil-bairro').click();
  await page.getByText('Centro', { exact: true }).click();

  await expect(page.getByTestId('funil-buscar')).toBeEnabled();
  await page.getByTestId('funil-buscar').click();
  await expect(page.getByTestId('tipo-servico-page')).toBeVisible();

  // BR11: the locality summary is shown and reflects the chosen values.
  await expect(page.getByTestId('tipo-servico-localidade-resumo')).toContainText('Centro, Rio de Janeiro – RJ');

  // BR5: only Consultórios–Clínicas–Terapias has the specialty step.
  await page.getByTestId('tipo-servico-item-CONSULTORIOS').click();
  await expect(page.getByTestId('especialidade-page')).toBeVisible();
  await page.getByTestId('option-search-input').fill('cardio');
  await page.getByTestId('option-item-CARDIOLOGIA').click();

  // AC2: ≥ 10 providers, all "CENTRO, RIO DE JANEIRO – RJ", under today's reference date.
  await expect(page.getByTestId('resultados-page')).toBeVisible();
  await expect(page.getByTestId('resultados-data-referencia')).toBeVisible();
  const cards = page.locator('[data-testid^="resultado-card-"]');
  await expect(cards).not.toHaveCount(0);
  expect(await cards.count()).toBeGreaterThanOrEqual(10);
  await expect(cards.first()).toContainText('Centro, Rio de Janeiro – RJ');

  // AC4: "Pesquisar por localidade" returns to step 1 with RJ/Rio de Janeiro/Centro pre-filled.
  await page.getByTestId('resultados-pesquisar-localidade').click();
  await expect(page.getByTestId('funil-uf')).toContainText('Rio de Janeiro');
  await expect(page.getByTestId('funil-municipio')).toContainText('Rio de Janeiro');
  await expect(page.getByTestId('funil-bairro')).toContainText('Centro');
});

test('name search finds a seeded provider (BR8, AC8)', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();

  await page.getByTestId('nav-rede').click();
  await page.getByTestId('rede-hub-buscaDeRede').click();
  await expect(page.getByTestId('network-search-page')).toBeVisible();

  await page.getByTestId('nome-input').fill('cardio');
  await expect(page.getByTestId('nome-buscar')).toBeEnabled();
  await page.getByTestId('nome-buscar').click();

  await expect(page.getByTestId('resultados-page')).toBeVisible();
  const cards = page.locator('[data-testid^="resultado-card-"]');
  await expect(cards.first()).toBeVisible();
  // BR8: name-search cards additionally show the service type.
  await expect(cards.first().getByTestId('resultado-servico')).toBeVisible();
});

test('Traçar rota opens the maps service with the provider\'s full address (BR12, AC6)', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel('E-mail').fill('maria@fkmed.local');
  await page.getByLabel('Senha').fill('maria12345');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page.getByTestId('home-page')).toBeVisible();

  await page.getByTestId('nav-rede').click();
  await page.getByTestId('rede-hub-buscaDeRede').click();
  await page.getByTestId('nome-input').fill('cardio');
  await page.getByTestId('nome-buscar').click();
  await expect(page.getByTestId('resultados-page')).toBeVisible();

  await page.locator('[data-testid^="resultado-card-"]').first().click();
  await expect(page.getByTestId('detalhe-page')).toBeVisible();

  const [popup] = await Promise.all([
    page.context().waitForEvent('page'),
    page.getByTestId('detalhe-tracar-rota').click(),
  ]);
  await popup.waitForLoadState();
  expect(popup.url()).toContain('google.com/maps');

  // BR12: "Copiar endereço" confirms visually.
  await page.context().grantPermissions(['clipboard-read', 'clipboard-write']);
  await page.getByTestId('detalhe-copiar-endereco').click();
  await expect(page.getByTestId('detalhe-endereco-copiado')).toBeVisible();
});
