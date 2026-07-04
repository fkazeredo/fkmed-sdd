import { defineConfig, devices } from '@playwright/test';

/**
 * E2E against the ISOLATED stack only (compose.e2e.yaml, frontend on :4201 — tmpfs Postgres,
 * fresh seed per run). Never point this at the dev database (docs/architecture/testing.md).
 *
 * `workers: 1`: the isolated stack resets its data once per `docker compose up`, not once per
 * spec file — every file shares the same live Postgres and the same two seeded beneficiaries
 * (MARIA, PEDRO). `seguranca-conta.spec.ts` mutates and eventually locks MARIA's account, so spec
 * files MUST run one at a time, in a stable (alphabetical) order, never concurrently — otherwise a
 * password change or lockout in one file races a login in another. Keep new account-mutating
 * specs named so they sort after any file that still needs the account to log in normally.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  retries: process.env['CI'] ? 1 : 0,
  workers: 1,
  reporter: process.env['CI'] ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:4201',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
