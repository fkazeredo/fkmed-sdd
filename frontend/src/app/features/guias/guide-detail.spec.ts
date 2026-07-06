import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { GuideDetail as GuideDetailModel, GuidesApi } from './guias.api';
import { GuideDetail } from './guide-detail';

const AUTHORIZED_GUIDE: GuideDetailModel = {
  id: 'guide-1',
  number: 'GU-0001',
  type: 'CONSULTA',
  requestingProvider: 'Dr. João',
  requestedAt: '2026-07-01',
  status: 'AUTORIZADA',
  items: [{ tussCode: '10101012', description: 'Consulta médica', quantity: 1, status: 'AUTORIZADO' }],
  authPassword: 'AUT-482913',
  authValidUntil: '2026-08-03',
};

const DENIED_GUIDE: GuideDetailModel = {
  id: 'guide-3',
  number: 'GU-0003',
  type: 'INTERNACAO',
  requestingProvider: 'Hospital São Lucas',
  requestedAt: '2026-06-10',
  status: 'NEGADA',
  items: [{ tussCode: '30101012', description: 'Internação clínica', quantity: 1, status: 'NEGADO' }],
  denialReason: 'Documentação insuficiente',
};

function routeWithId(id: string | null): Partial<ActivatedRoute> {
  return { snapshot: { paramMap: { get: () => id } } as unknown as ActivatedRoute['snapshot'] };
}

describe('GuideDetail (SPEC-0012 BR5/BR7)', () => {
  let fixture: ComponentFixture<GuideDetail>;
  let api: { getGuide: ReturnType<typeof vi.fn> };

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function setup(id = 'guide-1'): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [GuideDetail],
      providers: [
        provideRouter([]),
        provideI18n(),
        { provide: GuidesApi, useValue: api },
        { provide: ActivatedRoute, useValue: routeWithId(id) },
      ],
    }).compileComponents();
    TestBed.inject(BeneficiaryContextService).active.set({
      beneficiaryId: 'maria-id',
      firstName: 'MARIA',
      role: 'TITULAR',
    });
    fixture = TestBed.createComponent(GuideDetail);
    fixture.detectChanges();
  }

  beforeEach(() => {
    api = { getGuide: vi.fn().mockReturnValue(of(AUTHORIZED_GUIDE)) };
  });

  it('passes the id and the active beneficiaryId to GET /api/guides/{id} (required param)', async () => {
    await setup('guide-1');
    expect(api.getGuide).toHaveBeenCalledWith('guide-1', 'maria-id');
  });

  it('AC3 (BR5): shows the authorization password and its validity for an authorized guide', async () => {
    await setup();
    const senha = el().querySelector('[data-testid="guia-detalhe-senha"]');
    const validade = el().querySelector('[data-testid="guia-detalhe-validade"]');
    expect(senha?.textContent).toContain('AUT-482913');
    expect(validade?.textContent).toContain('03/08/2026');
    expect(el().querySelector('[data-testid="guia-detalhe-motivo-negacao"]')).toBeNull();
  });

  it('BR5: shows the items table (TUSS code, description, quantity, item status)', async () => {
    await setup();
    const row = el().querySelector('[data-testid="guia-detalhe-item-0"]') as HTMLElement;
    expect(row.textContent).toContain('10101012');
    expect(row.textContent).toContain('Consulta médica');
    expect(row.textContent).toContain('1');
  });

  it('AC3 (BR5): shows the denial reason for a NEGADA guide, no password block', async () => {
    api.getGuide.mockReturnValue(of(DENIED_GUIDE));
    await setup('guide-3');
    expect(el().querySelector('[data-testid="guia-detalhe-motivo-negacao"]')?.textContent).toContain(
      'Documentação insuficiente',
    );
    expect(el().querySelector('[data-testid="guia-detalhe-senha"]')).toBeNull();
  });

  it('BR7: shows the expired-authorization notice when authExpired is true', async () => {
    api.getGuide.mockReturnValue(of({ ...AUTHORIZED_GUIDE, authExpired: true }));
    await setup();
    expect(el().querySelector('[data-testid="guia-detalhe-autorizacao-expirada"]')).not.toBeNull();
  });

  it('does not show the expired-authorization notice when authExpired is absent/false', async () => {
    await setup();
    expect(el().querySelector('[data-testid="guia-detalhe-autorizacao-expirada"]')).toBeNull();
  });

  it('a 404 guide.not-found renders the dedicated not-found state', async () => {
    api.getGuide.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404, error: { code: 'guide.not-found' } })),
    );
    await setup();
    expect(el().querySelector('[data-testid="guia-detalhe-nao-encontrada"]')).not.toBeNull();
  });

  it('an unexpected failure renders the generic retry-capable error state', async () => {
    api.getGuide.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await setup();
    expect(el().querySelector('[data-testid="guia-detalhe-erro"]')).not.toBeNull();
    api.getGuide.mockReturnValue(of(AUTHORIZED_GUIDE));
    (el().querySelector('[data-testid="guia-detalhe-retry"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="guia-detalhe-erro"]')).toBeNull();
    expect(el().querySelector('[data-testid="guia-detalhe-numero"]')?.textContent).toContain('GU-0001');
  });
});
