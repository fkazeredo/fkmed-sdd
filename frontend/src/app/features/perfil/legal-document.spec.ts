import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { LegalDocumentsCurrent } from '../../core/legal/legal.api';
import { LegalDocumentPage } from './legal-document';

const CURRENT: LegalDocumentsCurrent = {
  terms: { version: '2.0', publishedAt: '2026-06-01', acceptedByMe: true, body: 'Texto dos termos.' },
  privacy: { version: '1.3', publishedAt: '2026-05-10', acceptedByMe: true, body: 'Texto de privacidade.' },
};

describe('LegalDocumentPage (SPEC-0006 BR8)', () => {
  let http: HttpTestingController;

  async function setup(type: 'TERMS' | 'PRIVACY') {
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
    http.expectOne('/api/legal-documents/current').flush(CURRENT);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => http.verify());

  it('shows the Terms with version, publication date and body', async () => {
    const fixture = await setup('TERMS');
    const el = fixture.nativeElement as HTMLElement;
    expect(fixture.componentInstance.doc()?.version).toBe('2.0');
    expect(el.querySelector('[data-testid="legal-meta"]')?.textContent).toContain('2.0');
    expect(el.querySelector('[data-testid="legal-meta"]')?.textContent).toContain('01/06/2026');
    expect(el.querySelector('[data-testid="legal-body"]')?.textContent).toContain('Texto dos termos.');
  });

  it('shows the Privacy document for the PRIVACY route type', async () => {
    const fixture = await setup('PRIVACY');
    expect(fixture.componentInstance.doc()?.version).toBe('1.3');
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="legal-body"]')?.textContent).toContain(
      'Texto de privacidade.',
    );
  });
});
