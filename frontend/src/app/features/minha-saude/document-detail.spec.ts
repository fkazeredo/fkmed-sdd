import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { ClinicalDocumentDetail, ClinicalDocumentsApi } from './clinical-documents.api';
import { DocumentDetail } from './document-detail';

const EXAM_ORDER: ClinicalDocumentDetail = {
  id: 'doc-exame',
  type: 'EXAM_ORDER',
  professional: 'Dra. Ana Souza',
  crm: 'CRM 12345 RJ',
  issuedAt: '2026-07-01',
  beneficiary: 'PEDRO',
  validUntil: '2026-09-29',
  expired: false,
  exams: [{ name: 'Hemograma completo', tuss: '40304361' }],
  clinicalIndication: 'Avaliação de rotina.',
};

const REFERRAL: ClinicalDocumentDetail = {
  id: 'doc-encaminhamento',
  type: 'REFERRAL',
  professional: 'Dr. Carlos Lima',
  crm: 'CRM 98765 RJ',
  issuedAt: '2026-06-20',
  beneficiary: 'MARIA',
  validUntil: '2026-09-18',
  expired: false,
  specialtyCode: 'CARDIOLOGIA',
  specialtyName: 'Cardiologia',
  reason: 'Avaliação cardiológica de rotina.',
};

const PRESCRIPTION: ClinicalDocumentDetail = {
  id: 'doc-receita',
  type: 'PRESCRIPTION',
  professional: 'Dra. Ana Souza',
  crm: 'CRM 12345 RJ',
  issuedAt: '2026-07-04',
  beneficiary: 'PEDRO',
  validUntil: '2026-08-03',
  expired: false,
  medications: [{ medication: 'Amoxicilina 500mg', posology: '1 cápsula a cada 8h por 7 dias', guidance: 'Tomar com alimento.' }],
};

const SICK_NOTE: ClinicalDocumentDetail = {
  id: 'doc-atestado',
  type: 'SICK_NOTE',
  professional: 'Dr. João',
  crm: 'CRM 54321 RJ',
  issuedAt: '2026-07-01',
  beneficiary: 'MARIA',
  validUntil: null,
  expired: false,
  periodStart: '2026-07-01',
  periodEnd: '2026-07-03',
  cid: 'J11',
  notes: 'Repouso domiciliar por 3 dias.',
};

describe('DocumentDetail (SPEC-0011 BR6/BR7, DL-0020)', () => {
  let fixture: ComponentFixture<DocumentDetail>;
  let api: { getDocument: ReturnType<typeof vi.fn>; downloadPdf: ReturnType<typeof vi.fn> };
  let router: Router;

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function setup(id = 'doc-1'): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [DocumentDetail],
      providers: [
        provideI18n(),
        { provide: ClinicalDocumentsApi, useValue: api },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id }) } } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentDetail);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  }

  beforeEach(() => {
    api = { getDocument: vi.fn().mockReturnValue(of(EXAM_ORDER)), downloadPdf: vi.fn() };
  });

  it('BR6: renders the common header — type, issue date, professional+CRM, beneficiary, validity', async () => {
    await setup();
    expect(el().querySelector('[data-testid="detalhe-profissional"]')?.textContent).toContain('Dra. Ana Souza');
    expect(el().querySelector('[data-testid="detalhe-profissional"]')?.textContent).toContain('CRM 12345 RJ');
    expect(el().querySelector('[data-testid="detalhe-data-emissao"]')?.textContent).toContain('01/07/2026');
    expect(el().querySelector('[data-testid="detalhe-beneficiario"]')?.textContent).toContain('PEDRO');
    expect(el().querySelector('[data-testid="detalhe-validade"]')?.textContent).toContain('29/09/2026');
  });

  it('BR6: exam order body lists exams with TUSS and the clinical indication', async () => {
    await setup();
    const item = el().querySelector('[data-testid="detalhe-exame-0"]');
    expect(item?.textContent).toContain('Hemograma completo');
    expect(item?.textContent).toContain('40304361');
    expect(el().querySelector('[data-testid="detalhe-indicacao-clinica"]')?.textContent).toContain(
      'Avaliação de rotina.',
    );
  });

  it('BR6/AC4: referral body shows the target specialty, reason and "Agendar consulta" navigates with the specialty pre-selected', async () => {
    api.getDocument.mockReturnValue(of(REFERRAL));
    await setup();
    expect(el().querySelector('[data-testid="detalhe-especialidade-alvo"]')?.textContent).toContain('Cardiologia');
    expect(el().querySelector('[data-testid="detalhe-motivo"]')?.textContent).toContain(
      'Avaliação cardiológica de rotina.',
    );
    (el().querySelector('[data-testid="detalhe-agendar-consulta"]') as HTMLElement).click();
    expect(router.navigate).toHaveBeenCalledWith(['/agendamento/consulta'], {
      queryParams: { especialidade: 'CARDIOLOGIA' },
    });
  });

  it('BR6: prescription body lists medications with posology and guidance', async () => {
    api.getDocument.mockReturnValue(of(PRESCRIPTION));
    await setup();
    const item = el().querySelector('[data-testid="detalhe-medicacao-0"]');
    expect(item?.textContent).toContain('Amoxicilina 500mg');
    expect(item?.textContent).toContain('1 cápsula a cada 8h por 7 dias');
    expect(item?.textContent).toContain('Tomar com alimento.');
  });

  it('BR6/DL-0020: sick note body shows the leave period, the CID and notes (CID IS displayed)', async () => {
    api.getDocument.mockReturnValue(of(SICK_NOTE));
    await setup();
    expect(el().querySelector('[data-testid="detalhe-periodo-afastamento"]')?.textContent).toContain('01/07/2026');
    expect(el().querySelector('[data-testid="detalhe-periodo-afastamento"]')?.textContent).toContain('03/07/2026');
    expect(el().querySelector('[data-testid="detalhe-cid"]')?.textContent).toContain('J11');
    expect(el().querySelector('[data-testid="detalhe-notas"]')?.textContent).toContain(
      'Repouso domiciliar por 3 dias.',
    );
    // BR4: a sick note has no validity badge.
    expect(el().querySelector('[data-testid="detalhe-validade"]')).toBeNull();
  });

  it('BR7: "Baixar PDF" downloads the blob via a triggered browser download', async () => {
    (URL as unknown as { createObjectURL: () => string }).createObjectURL = vi.fn().mockReturnValue('blob:mock');
    (URL as unknown as { revokeObjectURL: () => void }).revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    const pdfBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
    api.downloadPdf.mockReturnValue(of(pdfBlob));
    await setup();

    (el().querySelector('[data-testid="detalhe-baixar-pdf"]') as HTMLElement).click();
    fixture.detectChanges();

    expect(api.downloadPdf).toHaveBeenCalledWith('doc-exame');
    expect(URL.createObjectURL).toHaveBeenCalledWith(pdfBlob);
    expect(HTMLAnchorElement.prototype.click).toHaveBeenCalled();
  });

  it('BR5: an expired document still shows "Baixar PDF" (expired-still-downloadable)', async () => {
    api.getDocument.mockReturnValue(of({ ...EXAM_ORDER, expired: true }));
    await setup();
    expect(el().querySelector('[data-testid="detalhe-validade"]')?.textContent).toContain('Expirado');
    expect(el().querySelector('[data-testid="detalhe-baixar-pdf"]')).not.toBeNull();
  });

  it('shows an inline error when the PDF fails to generate', async () => {
    api.downloadPdf.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await setup();
    (el().querySelector('[data-testid="detalhe-baixar-pdf"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="detalhe-pdf-erro"]')).not.toBeNull();
  });

  it('maps a 404 document.not-found to a dedicated state (existence not revealed)', async () => {
    api.getDocument.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'document.not-found' }, status: 404 })),
    );
    await setup();
    expect(el().querySelector('[data-testid="detalhe-nao-encontrado"]')).not.toBeNull();
  });

  it('shows a generic retry-capable error on an unexpected failure', async () => {
    api.getDocument.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await setup();
    expect(el().querySelector('[data-testid="detalhe-erro"]')).not.toBeNull();
    api.getDocument.mockReturnValue(of(EXAM_ORDER));
    (el().querySelector('[data-testid="detalhe-retry"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="detalhe-erro"]')).toBeNull();
  });
});
