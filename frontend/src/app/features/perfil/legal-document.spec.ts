import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { LegalDocumentDetail } from '../../core/legal/legal.api';
import { LegalDocumentPage } from './legal-document';

const TERMS_DOC: LegalDocumentDetail = {
  type: 'TERMS',
  version: '2.0',
  publishedAt: '2026-06-01',
  body: 'Texto dos termos.',
};
const PRIVACY_DOC: LegalDocumentDetail = {
  type: 'PRIVACY',
  version: '1.3',
  publishedAt: '2026-05-10',
  body: 'Texto de privacidade.',
};

describe('LegalDocumentPage (SPEC-0006 BR8)', () => {
  let http: HttpTestingController;

  async function setup(type: 'TERMS' | 'PRIVACY', doc: LegalDocumentDetail) {
    await TestBed.configureTestingModule({
      imports: [LegalDocumentPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideI18n(),
        { provide: ActivatedRoute, useValue: { snapshot: { data: { type } } } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(LegalDocumentPage);
    await fixture.whenStable();
    http.expectOne(`/api/legal-documents/${type}`).flush(doc);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => http.verify());

  it('fetches the Terms text from GET /api/legal-documents/TERMS and shows version, date and body', async () => {
    const fixture = await setup('TERMS', TERMS_DOC);
    const el = fixture.nativeElement as HTMLElement;
    expect(fixture.componentInstance.doc()?.version).toBe('2.0');
    expect(el.querySelector('[data-testid="legal-meta"]')?.textContent).toContain('2.0');
    expect(el.querySelector('[data-testid="legal-meta"]')?.textContent).toContain('01/06/2026');
    expect(el.querySelector('[data-testid="legal-body"]')?.textContent).toContain('Texto dos termos.');
  });

  it('fetches the Privacy document for the PRIVACY route type', async () => {
    const fixture = await setup('PRIVACY', PRIVACY_DOC);
    expect(fixture.componentInstance.doc()?.version).toBe('1.3');
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="legal-body"]')?.textContent).toContain(
      'Texto de privacidade.',
    );
  });
});
