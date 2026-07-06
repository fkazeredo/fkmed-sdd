import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { ClinicalDocumentCard, ClinicalDocumentsApi } from './clinical-documents.api';
import { DocumentList } from './document-list';

const PEDRO_PRESCRIPTION: ClinicalDocumentCard = {
  id: 'doc-presc',
  type: 'PRESCRIPTION',
  professional: 'Dra. Ana Souza',
  crm: 'CRM 12345 RJ',
  issuedAt: '2026-07-04',
  beneficiary: 'PEDRO',
  validUntil: '2026-08-03',
  expired: false,
};
const EXPIRED_PRESCRIPTION: ClinicalDocumentCard = {
  ...PEDRO_PRESCRIPTION,
  id: 'doc-expired',
  issuedAt: '2026-05-01',
  validUntil: '2026-05-31',
  expired: true,
};
const SICK_NOTE: ClinicalDocumentCard = {
  id: 'doc-atestado',
  type: 'SICK_NOTE',
  professional: 'Dr. João',
  crm: 'CRM 54321 RJ',
  issuedAt: '2026-07-05',
  beneficiary: 'PEDRO',
  validUntil: null,
  expired: false,
};

function routeWithData(data: Record<string, unknown>): Partial<ActivatedRoute> {
  return { snapshot: { data } as ActivatedRoute['snapshot'] };
}

describe('DocumentList (SPEC-0011 BR2/BR4/BR5)', () => {
  let fixture: ComponentFixture<DocumentList>;
  let api: { getDocuments: ReturnType<typeof vi.fn> };

  const context = {
    accessible: () => [
      { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' as const },
      { beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' as const },
    ],
  };

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function setup(categories: string[] = ['PRESCRIPTION', 'SICK_NOTE']): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [DocumentList],
      providers: [
        provideRouter([]),
        provideI18n(),
        { provide: ClinicalDocumentsApi, useValue: api },
        { provide: BeneficiaryContextService, useValue: context },
        {
          provide: ActivatedRoute,
          useValue: routeWithData({ categories, titleKey: 'minhaSaude.receituarios.title' }),
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentList);
    fixture.detectChanges();
  }

  beforeEach(() => {
    api = { getDocuments: vi.fn().mockReturnValue(of({ items: [] })) };
  });

  it('BR2: loads with the default filters — beneficiaryId "all"', async () => {
    api.getDocuments.mockReturnValue(of({ items: [PEDRO_PRESCRIPTION] }));
    await setup(['PRESCRIPTION']);
    expect(api.getDocuments).toHaveBeenCalledWith(
      expect.objectContaining({ category: 'PRESCRIPTION', beneficiaryId: 'all' }),
    );
  });

  it('merges + re-sorts most-recent-first when the screen covers 2 categories (PRESCRIPTION + SICK_NOTE)', async () => {
    api.getDocuments.mockImplementation((filters: { category: string }) =>
      of({ items: filters.category === 'PRESCRIPTION' ? [PEDRO_PRESCRIPTION] : [SICK_NOTE] }),
    );
    await setup(['PRESCRIPTION', 'SICK_NOTE']);
    expect(api.getDocuments).toHaveBeenCalledTimes(2);
    const cardIds = Array.from(el().querySelectorAll('a[data-testid^="documento-card-"]')).map((n) =>
      n.getAttribute('data-testid'),
    );
    // SICK_NOTE issuedAt 2026-07-05 is more recent than the PRESCRIPTION's 2026-07-04.
    expect(cardIds).toEqual(['documento-card-doc-atestado', 'documento-card-doc-presc']);
  });

  it('BR2: filtering by beneficiary re-queries every underlying category with that id', async () => {
    await setup(['PRESCRIPTION', 'SICK_NOTE']);
    api.getDocuments.mockClear();
    fixture.componentInstance.onBeneficiaryChange('pedro-id');
    expect(api.getDocuments).toHaveBeenCalledWith(expect.objectContaining({ beneficiaryId: 'pedro-id' }));
    expect(api.getDocuments).toHaveBeenCalledTimes(2);
  });

  it('BR2: switching to a named period re-queries immediately', async () => {
    await setup(['PRESCRIPTION']);
    api.getDocuments.mockClear();
    fixture.componentInstance.onPeriodChange('P365D');
    expect(api.getDocuments).toHaveBeenCalledWith(expect.objectContaining({ period: 'P365D' }));
  });

  it('BR2: a custom range only queries once both dates are set and "Aplicar" is used', async () => {
    await setup(['PRESCRIPTION']);
    api.getDocuments.mockClear();
    fixture.componentInstance.onPeriodChange('CUSTOM');
    expect(api.getDocuments).not.toHaveBeenCalled();
    fixture.componentInstance['customFrom'].set('2026-01-01');
    fixture.componentInstance['customTo'].set('2026-07-01');
    fixture.componentInstance.applyCustomRange();
    expect(api.getDocuments).toHaveBeenCalledWith(
      expect.objectContaining({ from: '2026-01-01', to: '2026-07-01' }),
    );
    const call = api.getDocuments.mock.calls[0][0];
    expect(call.period).toBeUndefined();
  });

  it('drives the beneficiary and custom-period filters through the real DOM controls', async () => {
    await setup(['PRESCRIPTION']);
    api.getDocuments.mockClear();

    const beneficiarySelect = el().querySelector('[data-testid="lista-filtro-beneficiario"]') as HTMLSelectElement;
    beneficiarySelect.value = 'pedro-id';
    beneficiarySelect.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(api.getDocuments).toHaveBeenCalledWith(expect.objectContaining({ beneficiaryId: 'pedro-id' }));

    const periodSelect = el().querySelector('[data-testid="lista-filtro-periodo"]') as HTMLSelectElement;
    periodSelect.value = 'CUSTOM';
    periodSelect.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const fromInput = el().querySelector('[data-testid="lista-filtro-de"]') as HTMLInputElement;
    const toInput = el().querySelector('[data-testid="lista-filtro-ate"]') as HTMLInputElement;
    expect(fromInput).not.toBeNull();
    expect(toInput).not.toBeNull();
    fromInput.value = '2026-01-01';
    fromInput.dispatchEvent(new Event('input'));
    toInput.value = '2026-07-01';
    toInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    api.getDocuments.mockClear();
    (el().querySelector('[data-testid="lista-filtro-aplicar"]') as HTMLButtonElement).click();
    expect(api.getDocuments).toHaveBeenCalledWith(
      expect.objectContaining({ from: '2026-01-01', to: '2026-07-01' }),
    );
  });

  it('BR4/BR5: renders "Válido até" for a valid document and "Expirado" for an expired one, both downloadable via the detail link', async () => {
    api.getDocuments.mockReturnValue(of({ items: [PEDRO_PRESCRIPTION, EXPIRED_PRESCRIPTION] }));
    await setup(['PRESCRIPTION']);
    const validCard = el().querySelector('[data-testid="documento-card-doc-presc"]') as HTMLElement;
    const expiredCard = el().querySelector('[data-testid="documento-card-doc-expired"]') as HTMLElement;
    expect(validCard.querySelector('[data-testid="documento-card-validade"]')?.textContent).toContain(
      '03/08/2026',
    );
    expect(expiredCard.querySelector('[data-testid="documento-card-validade"]')?.textContent).toContain(
      'Expirado',
    );
    expect(expiredCard.getAttribute('href')).toContain('/minha-saude/documento/doc-expired');
  });

  it('BR4: a sick note (validUntil null) shows no validity badge', async () => {
    api.getDocuments.mockReturnValue(of({ items: [SICK_NOTE] }));
    await setup(['SICK_NOTE']);
    const card = el().querySelector('[data-testid="documento-card-doc-atestado"]') as HTMLElement;
    expect(card.querySelector('[data-testid="documento-card-validade"]')).toBeNull();
  });

  it('shows the empty state when there are no documents for the filter', async () => {
    await setup(['PRESCRIPTION']);
    expect(el().querySelector('[data-testid="lista-vazio"]')).not.toBeNull();
  });

  it('shows a retry-capable error state on failure', async () => {
    api.getDocuments.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await setup(['PRESCRIPTION']);
    expect(el().querySelector('[data-testid="lista-erro"]')).not.toBeNull();
    api.getDocuments.mockReturnValue(of({ items: [PEDRO_PRESCRIPTION] }));
    (el().querySelector('[data-testid="lista-retry"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="lista-erro"]')).toBeNull();
    expect(el().querySelector('[data-testid="documento-card-doc-presc"]')).not.toBeNull();
  });

  it('each card links to its detail route', async () => {
    api.getDocuments.mockReturnValue(of({ items: [PEDRO_PRESCRIPTION] }));
    await setup(['PRESCRIPTION']);
    const link = el().querySelector('[data-testid="documento-card-doc-presc"]') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/minha-saude/documento/doc-presc');
  });
});
