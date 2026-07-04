import { authConfigFor } from './auth.service';

/**
 * Regression (review finding M3): requireHttps must not be unconditionally false. Production
 * builds enforce https ('remoteOnly' keeps localhost E2E/prod-smoke working over http);
 * only the dev build fully relaxes it.
 */
describe('authConfigFor', () => {
  it('enforces https on production builds (remoteOnly: localhost stays allowed)', () => {
    const config = authConfigFor({ production: true, oidcIssuer: 'https://fkmed.example.com' });
    expect(config.requireHttps).toBe('remoteOnly');
  });

  it('relaxes https on dev builds only', () => {
    const config = authConfigFor({ production: false, oidcIssuer: 'http://localhost:8080' });
    expect(config.requireHttps).toBe(false);
  });

  it('binds the issuer from the environment', () => {
    const config = authConfigFor({ production: false, oidcIssuer: 'http://localhost:8080' });
    expect(config.issuer).toBe('http://localhost:8080');
    expect(config.clientId).toBe('fkmed-web');
    expect(config.responseType).toBe('code');
  });
});
