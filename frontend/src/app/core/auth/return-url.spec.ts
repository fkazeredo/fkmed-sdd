import { saveReturnUrl, takeReturnUrl } from './return-url';

/**
 * BR12/BR13: the intended route (the one a visitor was denied, or was on when the session
 * expired) is preserved across the full-page redirect to the AS login and back — sessionStorage
 * survives that round trip while surviving only the current tab (SPEC-0002 BR12/BR13).
 */
describe('return-url', () => {
  beforeEach(() => sessionStorage.clear());

  it('round-trips a saved url', () => {
    saveReturnUrl('/seguranca');
    expect(takeReturnUrl()).toBe('/seguranca');
  });

  it('consumes the url — a second read returns null', () => {
    saveReturnUrl('/meu-plano');
    takeReturnUrl();
    expect(takeReturnUrl()).toBeNull();
  });

  it('returns null when nothing was saved', () => {
    expect(takeReturnUrl()).toBeNull();
  });

  it('does not save the root path (nothing meaningful to return to)', () => {
    saveReturnUrl('/');
    expect(takeReturnUrl()).toBeNull();
  });

  it('does not save the session-expired notice route itself', () => {
    saveReturnUrl('/sessao-expirada');
    expect(takeReturnUrl()).toBeNull();
  });
});
