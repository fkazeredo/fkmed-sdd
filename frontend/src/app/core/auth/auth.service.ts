import { inject, Injectable } from '@angular/core';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

/** The environment facts the OIDC config depends on (pure — unit-testable). */
export interface AuthEnvironment {
  production: boolean;
  oidcIssuer: string;
}

/** OIDC client configuration against the embedded Authorization Server (SPEC-0001 BR8). */
export function authConfigFor(env: AuthEnvironment): AuthConfig {
  return {
    issuer: env.oidcIssuer,
    clientId: 'fkmed-web',
    responseType: 'code',
    scope: 'openid profile',
    redirectUri: window.location.origin + '/',
    postLogoutRedirectUri: window.location.origin + '/',
    // Production enforces https ('remoteOnly' keeps localhost smoke/E2E over http working);
    // only the dev build (ng serve against :8080) fully relaxes it (review finding M3).
    requireHttps: env.production ? 'remoteOnly' : false,
  };
}

/** The app's OIDC config, bound to the build environment. */
export function buildAuthConfig(): AuthConfig {
  return authConfigFor(environment);
}

/**
 * Thin facade over angular-oauth2-oidc: Authorization Code + PKCE against the embedded AS.
 * Components and guards depend on this service, never on OAuthService directly.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauth = inject(OAuthService);

  /** Configures OIDC and processes a login callback when present; runs at app bootstrap. */
  async init(): Promise<void> {
    this.oauth.configure(buildAuthConfig());
    try {
      await this.oauth.loadDiscoveryDocumentAndTryLogin();
    } catch (error) {
      // The guard retriggers the code flow on navigation; booting the shell is still useful.
      console.warn('OIDC discovery failed at bootstrap', error);
    }
  }

  isAuthenticated(): boolean {
    return this.oauth.hasValidAccessToken();
  }

  /** Starts the Authorization Code + PKCE flow (full-page redirect to the AS login). */
  login(): void {
    this.oauth.initCodeFlow();
  }

  logout(): void {
    this.oauth.logOut();
  }

  /** Display name of the logged user (OIDC subject). */
  username(): string {
    const claims = this.oauth.getIdentityClaims() as { sub?: string } | null;
    return claims?.sub ?? '';
  }

  /** The current OIDC access token (empty when none). The resource-server interceptor already
   * attaches it to HttpClient calls to `/api`; this accessor exists for the few requests made
   * outside HttpClient that must set the `Authorization` header themselves — notably the
   * telemedicine SSE stream via `fetch` (SPEC-0010, ADR-0016), since `EventSource` cannot. */
  accessToken(): string {
    return this.oauth.getAccessToken() ?? '';
  }
}
