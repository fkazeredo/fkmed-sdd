import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { takeReturnUrl } from './return-url';
import { sessionExpiryInterceptor } from './session-expiry.interceptor';

/**
 * BR12: a 401 on an authenticated /api/** call mid-use means the session expired. The interceptor
 * preserves the in-flight route (reusing the return-url helper the guard also uses — DL: single
 * restore path) and sends the user to the local notice; it never touches non-API calls or a 401
 * that happens while already on the notice screen (would loop).
 */
describe('sessionExpiryInterceptor', () => {
  let http: HttpTestingController;
  let httpClient: HttpClient;
  let router: { url: string; navigateByUrl: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    sessionStorage.clear();
    router = { url: '/seguranca', navigateByUrl: vi.fn() };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([sessionExpiryInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
  });

  afterEach(() => http.verify());

  it('on a 401 from /api/**, saves the current route and navigates to the notice', () => {
    let errored = false;
    httpClient.get('/api/plan/my-plan').subscribe({ error: () => (errored = true) });
    http.expectOne('/api/plan/my-plan').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(errored).toBe(true);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/sessao-expirada');
    expect(takeReturnUrl()).toBe('/seguranca');
  });

  it('ignores a 401 from a non-API call', () => {
    httpClient.get('/oauth2/userinfo').subscribe({ error: () => undefined });
    http.expectOne('/oauth2/userinfo').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('ignores other error statuses', () => {
    httpClient.get('/api/plan/my-plan').subscribe({ error: () => undefined });
    http.expectOne('/api/plan/my-plan').flush(null, { status: 500, statusText: 'Server Error' });

    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('does not loop when already on the session-expired screen', () => {
    router.url = '/sessao-expirada';
    httpClient.get('/api/plan/my-plan').subscribe({ error: () => undefined });
    http.expectOne('/api/plan/my-plan').flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
