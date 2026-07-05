import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { LegalDocumentDetail, LegalDocumentsCurrent } from '../../core/legal/legal.api';
import { LegalAcceptance } from './legal-acceptance';

const TERMS_PENDING: LegalDocumentsCurrent = {
  terms: { version: '3.0', publishedAt: '2026-07-01', acceptedByMe: false },
  privacy: { version: '1.0', publishedAt: '2026-01-01', acceptedByMe: true },
};

const BOTH_PENDING: LegalDocumentsCurrent = {
  terms: { version: '3.0', publishedAt: '2026-07-01', acceptedByMe: false },
  privacy: { version: '2.0', publishedAt: '2026-07-01', acceptedByMe: false },
};

const TERMS_DOC: LegalDocumentDetail = {
  type: 'TERMS',
  version: '3.0',
  publishedAt: '2026-07-01',
  body: 'Novos termos.',
};
const PRIVACY_DOC: LegalDocumentDetail = {
  type: 'PRIVACY',
  version: '2.0',
  publishedAt: '2026-07-01',
  body: 'Nova privacidade.',
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

  async function create(current: LegalDocumentsCurrent, docs: LegalDocumentDetail[]) {
    const fixture = TestBed.createComponent(LegalAcceptance);
    await fixture.whenStable();
    http.expectOne('/api/legal-documents/current').flush(current);
    await fixture.whenStable();
    for (const doc of docs) {
      http.expectOne(`/api/legal-documents/${doc.type}`).flush(doc);
    }
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('lists the pending document(s) with their fetched text (BR8)', async () => {
    const fixture = await create(TERMS_PENDING, [TERMS_DOC]);
    expect(fixture.componentInstance.pendingDocs().map((d) => d.type)).toEqual(['TERMS']);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="aceite-TERMS"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="aceite-TERMS"]')?.textContent).toContain('Novos termos.');
  });

  it('accepts sending the displayed version and resumes navigation to Home (AC5)', async () => {
    const fixture = await create(TERMS_PENDING, [TERMS_DOC]);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture.componentInstance.accept('TERMS', '3.0');
    const request = http.expectOne('/api/legal-documents/TERMS/accept');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ version: '3.0' });
    request.flush(null);

    expect(navigate).toHaveBeenCalledWith('/home');
  });

  it('does not resume until BOTH pending documents are accepted', async () => {
    const fixture = await create(BOTH_PENDING, [TERMS_DOC, PRIVACY_DOC]);
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture.componentInstance.accept('TERMS', '3.0');
    http.expectOne('/api/legal-documents/TERMS/accept').flush(null);
    expect(navigate).not.toHaveBeenCalled();
    expect(fixture.componentInstance.pendingDocs().map((d) => d.type)).toEqual(['PRIVACY']);

    fixture.componentInstance.accept('PRIVACY', '2.0');
    http.expectOne('/api/legal-documents/PRIVACY/accept').flush(null);
    expect(navigate).toHaveBeenCalledWith('/home');
  });

  it('on a 409 outdated version, shows the message and reloads the snapshot + text', async () => {
    const fixture = await create(TERMS_PENDING, [TERMS_DOC]);

    fixture.componentInstance.accept('TERMS', '3.0');
    http
      .expectOne('/api/legal-documents/TERMS/accept')
      .flush({ code: 'legal.version-outdated' }, { status: 409, statusText: 'Conflict' });

    expect(fixture.componentInstance.errorKey()).toBe('legal.version-outdated');
    // reload() re-fetches current, then the pending document texts.
    http.expectOne('/api/legal-documents/current').flush(TERMS_PENDING);
    await fixture.whenStable();
    http.expectOne('/api/legal-documents/TERMS').flush(TERMS_DOC);
  });
});
