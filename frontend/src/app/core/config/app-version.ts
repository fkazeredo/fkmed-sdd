import { InjectionToken } from '@angular/core';

/**
 * The product version string, injected from the build environment (SPEC-0006 BR10). Components
 * (the Perfil menu) read it through DI rather than importing `environment` directly, so tests can
 * pin a value and the "never hardcoded" rule is honoured — the concrete value is provided once in
 * `app.config.ts` from `environment.appVersion`.
 */
export const APP_VERSION = new InjectionToken<string>('APP_VERSION');
