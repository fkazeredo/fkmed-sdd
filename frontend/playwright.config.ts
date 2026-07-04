import { defineConfig, devices } from '@playwright/test';

/**
 * E2E against the ISOLATED stack only (compose.e2e.yaml, frontend on :4201 — tmpfs Postgres,
 * fresh seed per run). Never point this at the dev database (docs/architecture/testing.md).
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  retries: process.env['CI'] ? 1 : 0,
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
