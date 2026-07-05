import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { LegalDocumentsCurrent } from '../../core/legal/legal.api';
import { LegalAcceptance } from './legal-acceptance';

const TERMS_PENDING: LegalDocumentsCurrent = {
  terms: { version: '3.0', publishedAt: '2026-07-01', acceptedByMe: false, body: 'Novos termos.' },
  privacy: { version: '1.0', publishedAt: '2026-01-01', acceptedByMe: true },
};

const BOTH_PENDING: LegalDocumentsCurrent = {
  terms: { version: '3.0', publishedAt: '2026-07-01', acceptedByMe: false },
  privacy: { version: '2.0', publishedAt: '2026-07-01', acceptedByMe: false },
};

describe('LegalAcceptance (SPEC-0006 BR8, AC5)', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LegalAcceptance],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideI18n(), provideRouter([])],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function create(current: LegalDocumentsCurrent) {
    const fixture = TestBed.createComponent(LegalAcceptance);
    await fixture.whenStable();
    http.expectOne('/api/legal-documents/current').flush(current);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('lists the pending mandatory document(s) to accept', async () => {
    const fixture = await create(TERMS_PENDING);
    expect(fixture.componentInstance.pending().map((p) => p.type)).toEqual(['TERMS']);
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="aceite-TERMS"]')).not.toBeNull();
  });

  it('accepts, records the version and resumes navigation to Home (AC5)', async () => {
    const fixture = await create(TERMS_PENDING);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture.componentInstance.accept('TERMS');
    const request = http.expectOne('/api/legal-documents/TERMS/accept');
    expect(request.request.method).toBe('POST');
    request.flush(null);

    expect(navigate).toHaveBeenCalledWith('/home');
  });

  it('does not resume until BOTH pending documents are accepted', async () => {
    const fixture = await create(BOTH_PENDING);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture.componentInstance.accept('TERMS');
    http.expectOne('/api/legal-documents/TERMS/accept').flush(null);
    expect(navigate).not.toHaveBeenCalled();
    expect(fixture.componentInstance.pending().map((p) => p.type)).toEqual(['PRIVACY']);

    fixture.componentInstance.accept('PRIVACY');
    http.expectOne('/api/legal-documents/PRIVACY/accept').flush(null);
    expect(navigate).toHaveBeenCalledWith('/home');
  });

  it('on a 409 outdated version, shows the message and reloads the current snapshot', async () => {
    const fixture = await create(TERMS_PENDING);

    fixture.componentInstance.accept('TERMS');
    http
      .expectOne('/api/legal-documents/TERMS/accept')
      .flush({ code: 'legal.version-outdated' }, { status: 409, statusText: 'Conflict' });

    expect(fixture.componentInstance.errorKey()).toBe('legal.version-outdated');
    // reload() re-fetches the snapshot.
    http.expectOne('/api/legal-documents/current').flush(TERMS_PENDING);
  });
});
