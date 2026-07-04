import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideRouter } from '@angular/router';
import Aura from '@primeuix/themes/aura';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { providePrimeNG } from 'primeng/config';

import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { sessionExpiryInterceptor } from './core/auth/session-expiry.interceptor';
import { provideI18n } from './core/i18n/provide-i18n';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // withInterceptorsFromDi: angular-oauth2-oidc registers its Bearer interceptor via DI.
    // withInterceptors: our own functional interceptor (BR12 session-expiry, core/auth).
    provideHttpClient(withInterceptorsFromDi(), withInterceptors([sessionExpiryInterceptor])),
    provideAnimationsAsync(),
    providePrimeNG({ theme: { preset: Aura } }),
    provideOAuthClient({
      resourceServer: { allowedUrls: ['/api'], sendAccessToken: true },
    }),
    provideI18n(),
    provideAppInitializer(() => inject(AuthService).init()),
  ],
};
