import { defineConfig, devices } from '@playwright/test';

/**
 * E2E against the ISOLATED stack only (compose.e2e.yaml, frontend on :4201 — tmpfs Postgres,
 * fresh seed per run). Never point this at the dev database (docs/architecture/testing.md).
 *
 * `workers: 1`: the isolated stack resets its data once per `docker compose up`, not once per
 * spec file — every file shares the same live Postgres. `seguranca-conta.spec.ts` mutates and
 * eventually locks a disposable account seeded just for it (`seguranca-e2e@fkmed.local` — its own
 * family, never MARIA/PEDRO), so spec files MUST still run one at a time, never concurrently:
 * otherwise a password change or lockout in that file could race another spec that happens to
 * touch the same account, or two runs of the same file could collide over the one seeded account.
 * Keep any new account-mutating spec on its own disposable seeded account rather than a shared
 * canonical one (MARIA/PEDRO), so it never needs file-name/order coupling to stay isolated.
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
