import { inject, Injectable } from '@angular/core';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

/** OIDC client configuration against the embedded Authorization Server (SPEC-0001 BR8). */
export function buildAuthConfig(): AuthConfig {
  return {
    issuer: environment.oidcIssuer,
    clientId: 'fkmed-web',
    responseType: 'code',
    scope: 'openid profile',
    redirectUri: window.location.origin + '/',
    postLogoutRedirectUri: window.location.origin + '/',
    // Same-process AS; silent refresh via SSO session arrives with SPEC-0002's journeys.
    requireHttps: false,
  };
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
}
