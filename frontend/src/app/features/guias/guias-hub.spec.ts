import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { BeneficiarySummaryApi } from '../../core/context/beneficiary-summary.api';
import { GuideCard, GuidesApi, TokenResponse } from './guias.api';
import { GuiasHub } from './guias-hub';

const MARIA_ACCESSIBLE = { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' as const };
const PEDRO_ACCESSIBLE = { beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' as const };

const GUIDE_EM_ANALISE: GuideCard = {
  id: 'guide-1',
  number: 'GU-0001',
  type: 'CONSULTA',
  requestingProvider: 'Dr. João',
  requestedAt: '2026-07-01',
  status: 'EM_ANALISE',
};
const GUIDE_AUTORIZADA: GuideCard = {
  id: 'guide-2',
  number: 'GU-0002',
  type: 'SP_SADT',
  requestingProvider: 'Clínica Central',
  requestedAt: '2026-06-20',
  status: 'AUTORIZADA',
};
const GUIDE_NEGADA: GuideCard = {
  id: 'guide-3',
  number: 'GU-0003',
  type: 'INTERNACAO',
  requestingProvider: 'Hospital São Lucas',
  requestedAt: '2026-06-10',
  status: 'NEGADA',
};

// `expiresAt` is always computed relative to the real clock (the component ticks off `new Date()`,
// not a fixed instant) so these fixtures stay valid/expired regardless of when the suite runs.
const TOKEN_ACTIVE: TokenResponse = { code: '483920', expiresAt: new Date(Date.now() + 10 * 60_000).toISOString() };

/**
 * SPEC-0012 BR1-BR4/BR9-BR11 — GuiasHub: the one screen with the guides list + token sections.
 * Mocks GuidesApi/BeneficiarySummaryApi (frozen contract) — no dependency on a running backend.
 */
describe('GuiasHub', () => {
  let fixture: ComponentFixture<GuiasHub>;
  let api: {
    getGuides: ReturnType<typeof vi.fn>;
    getGuide: ReturnType<typeof vi.fn>;
    getCurrentToken: ReturnType<typeof vi.fn>;
    generateToken: ReturnType<typeof vi.fn>;
  };
  let summaryApi: { getBeneficiary: ReturnType<typeof vi.fn> };
  let context: BeneficiaryContextService;

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  async function setup(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [GuiasHub],
      providers: [
        provideRouter([]),
        provideI18n(),
        { provide: GuidesApi, useValue: api },
        { provide: BeneficiarySummaryApi, useValue: summaryApi },
      ],
    }).compileComponents();
    context = TestBed.inject(BeneficiaryContextService);
    context.accessible.set([MARIA_ACCESSIBLE, PEDRO_ACCESSIBLE]);
    fixture = TestBed.createComponent(GuiasHub);
    fixture.detectChanges();
    context.active.set(MARIA_ACCESSIBLE);
    fixture.detectChanges();
  }

  beforeEach(() => {
    api = {
      getGuides: vi.fn().mockReturnValue(of({ items: [] })),
      getGuide: vi.fn(),
      getCurrentToken: vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 }))),
      generateToken: vi.fn(),
    };
    summaryApi = {
      getBeneficiary: vi.fn().mockReturnValue(
        of({
          firstName: 'MARIA',
          fullName: 'MARIA CLARA',
          role: 'TITULAR',
          planName: 'PLANO PRATA',
          cardNumber: '001234567',
          avatarUrl: null,
        }),
      ),
    };
  });

  it('BR1: reloads guides/token/plan-strip on active-beneficiary switch', async () => {
    await setup();
    expect(api.getGuides).toHaveBeenCalledWith(expect.objectContaining({ beneficiaryId: 'maria-id' }));
    expect(summaryApi.getBeneficiary).toHaveBeenCalledWith('maria-id');

    api.getGuides.mockClear();
    summaryApi.getBeneficiary.mockClear();
    context.setActive('pedro-id');
    fixture.detectChanges();
    expect(api.getGuides).toHaveBeenCalledWith(expect.objectContaining({ beneficiaryId: 'pedro-id' }));
    expect(summaryApi.getBeneficiary).toHaveBeenCalledWith('pedro-id');
  });

  it('BR1: shows the plan strip with the active beneficiary plan/card', async () => {
    await setup();
    const strip = el().querySelector('[data-testid="guias-plano-strip"]') as HTMLElement;
    expect(strip.textContent).toContain('PLANO PRATA');
    expect(strip.textContent).toContain('001234567');
  });

  it('AC1 (BR2): renders 3 guides with distinct status badges', async () => {
    api.getGuides.mockReturnValue(of({ items: [GUIDE_EM_ANALISE, GUIDE_AUTORIZADA, GUIDE_NEGADA] }));
    await setup();
    expect(el().querySelector('[data-testid="guia-card-guide-1"]')).not.toBeNull();
    expect(el().querySelector('[data-testid="guia-card-guide-2"]')).not.toBeNull();
    expect(el().querySelector('[data-testid="guia-card-guide-3"]')).not.toBeNull();
    const badges = Array.from(el().querySelectorAll('[data-testid="guia-card-status"]')).map((n) => n.className);
    expect(new Set(badges).size).toBe(3);
  });

  it('AC2 (BR3): shows the empty state with "Atualizar informações" — never a blank screen', async () => {
    await setup();
    const empty = el().querySelector('[data-testid="guias-vazio"]') as HTMLElement;
    expect(empty).not.toBeNull();
    expect(empty.querySelector('[data-testid="guias-vazio-atualizar"]')).not.toBeNull();
    expect(empty.textContent).toContain('Nenhuma solicitação em andamento');
  });

  it('BR4: Atualizar re-fetches immediately with a loading indicator', async () => {
    await setup();
    api.getGuides.mockClear();
    api.getGuides.mockReturnValue(of({ items: [GUIDE_EM_ANALISE] }));
    (el().querySelector('[data-testid="guias-atualizar"]') as HTMLElement).click();
    expect(api.getGuides).toHaveBeenCalledTimes(1);
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="guia-card-guide-1"]')).not.toBeNull();
  });

  it('Filtrar toggles the filter panel; changing status re-queries', async () => {
    await setup();
    expect(el().querySelector('[data-testid="guias-filtros"]')).toBeNull();
    (el().querySelector('[data-testid="guias-filtrar"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="guias-filtros"]')).not.toBeNull();

    api.getGuides.mockClear();
    fixture.componentInstance.onStatusChange('NEGADA');
    expect(api.getGuides).toHaveBeenCalledWith(expect.objectContaining({ status: 'NEGADA' }));
  });

  it('AC4/BR9/BR11: generating a token shows the code + countdown; Copiar copies exactly the 6 digits', async () => {
    api.generateToken.mockReturnValue(of(TOKEN_ACTIVE));
    await setup();
    expect(el().querySelector('[data-testid="token-vazio"]')).not.toBeNull();

    (el().querySelector('[data-testid="token-gerar"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="token-codigo"]')?.textContent).toContain('483920');
    expect(el().querySelector('[data-testid="token-countdown"]')?.textContent).toContain('10:00');

    Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });
    (el().querySelector('[data-testid="token-copiar"]') as HTMLElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('483920');
    expect(el().querySelector('[data-testid="token-copiado"]')).not.toBeNull();
  });

  it('BR9: resumes an active token countdown from GET /api/tokens/current on load', async () => {
    api.getCurrentToken.mockReturnValue(of(TOKEN_ACTIVE));
    await setup();
    expect(el().querySelector('[data-testid="token-ativo"]')).not.toBeNull();
    expect(el().querySelector('[data-testid="token-codigo"]')?.textContent).toContain('483920');
  });

  it('a 404 token.none-active on load is treated as "no active token" — not an error toast', async () => {
    await setup();
    expect(el().querySelector('[data-testid="token-vazio"]')).not.toBeNull();
    expect(el().querySelector('[data-testid="token-erro"]')).toBeNull();
  });

  it('AC5/BR9: generating another token replaces the displayed code', async () => {
    api.getCurrentToken.mockReturnValue(of(TOKEN_ACTIVE));
    const NEW_TOKEN: TokenResponse = { code: '111222', expiresAt: new Date(Date.now() + 10 * 60_000).toISOString() };
    api.generateToken.mockReturnValue(of(NEW_TOKEN));
    await setup();
    expect(el().querySelector('[data-testid="token-codigo"]')?.textContent).toContain('483920');
    (el().querySelector('[data-testid="token-gerar-novo"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="token-codigo"]')?.textContent).toContain('111222');
    expect(el().querySelector('[data-testid="token-codigo"]')?.textContent).not.toContain('483920');
  });

  it('AC6/BR10: an expired token shows "Token expirado" and offers a new one, never the old code', async () => {
    const expiredToken: TokenResponse = { code: '483920', expiresAt: new Date(Date.now() - 60_000).toISOString() };
    api.getCurrentToken.mockReturnValue(of(expiredToken));
    await setup();
    expect(el().querySelector('[data-testid="token-expirado"]')).not.toBeNull();
    expect(el().querySelector('[data-testid="token-codigo"]')).toBeNull();
    expect(el().querySelector('[data-testid="token-gerar-novo"]')).not.toBeNull();
  });
});
