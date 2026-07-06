import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { MissingTranslationHandler } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { provideI18n, ReportMissingTranslationHandler } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { BeneficiarySummaryApi } from '../../core/context/beneficiary-summary.api';
import { GuideDetail as GuideDetailModel, GuidesApi, TokenResponse } from './guias.api';
import { GuiasHub } from './guias-hub';
import { GuideDetail } from './guide-detail';

/**
 * SPEC-0012: every visible Guias e Token UI string resolves from the pt-BR bundle. Renders both
 * screens through their branches with a recording MissingTranslationHandler; a single missing key
 * fails. Kept inside the feature (the central i18n-completeness spec is core scope, mirrors
 * features/telemedicina/telemedicina-i18n.spec.ts) — the architect may fold these in at integration.
 */
describe('guias i18n completeness (pt-BR)', () => {
  const TOKEN: TokenResponse = { code: '483920', expiresAt: new Date(Date.now() + 10 * 60_000).toISOString() };
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
    ...AUTHORIZED_GUIDE,
    id: 'guide-3',
    status: 'NEGADA',
    authPassword: undefined,
    authValidUntil: undefined,
    denialReason: 'Documentação insuficiente',
  };

  let guidesApi: Record<string, ReturnType<typeof vi.fn>>;
  let summaryApi: Record<string, ReturnType<typeof vi.fn>>;

  function missing(): string[] {
    return Array.from((TestBed.inject(MissingTranslationHandler) as ReportMissingTranslationHandler).missing);
  }

  beforeEach(() => {
    guidesApi = {
      getGuides: vi.fn().mockReturnValue(of([])),
      getGuide: vi.fn().mockReturnValue(of(AUTHORIZED_GUIDE)),
      getCurrentToken: vi.fn().mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 }))),
      generateToken: vi.fn().mockReturnValue(of(TOKEN)),
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

  it('renders the hub — empty guides, filters, token empty/active/expired states — without a missing key', async () => {
    await TestBed.configureTestingModule({
      imports: [GuiasHub],
      providers: [
        provideRouter([]),
        provideI18n(),
        { provide: GuidesApi, useValue: guidesApi },
        { provide: BeneficiarySummaryApi, useValue: summaryApi },
      ],
    }).compileComponents();
    const context = TestBed.inject(BeneficiaryContextService);
    context.accessible.set([
      { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' },
      { beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' },
    ]);
    const hub = TestBed.createComponent(GuiasHub);
    hub.detectChanges();
    context.active.set({ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' });
    hub.detectChanges(); // empty state (BR3) + plan strip + token empty (BR9)

    hub.componentInstance.toggleFilters();
    hub.detectChanges(); // filter panel (status + period selects)
    hub.componentInstance.onPeriodChange('LAST_12M');
    hub.detectChanges(); // period re-query

    hub.componentInstance.generateToken();
    hub.detectChanges(); // token active + countdown
    Object.assign(navigator, { clipboard: { writeText: vi.fn().mockResolvedValue(undefined) } });
    hub.componentInstance.copyToken();
    await hub.whenStable();
    hub.detectChanges(); // copy confirmation

    // Expired-token branch: force the signal directly (real countdown reaching zero is exercised
    // in token-time.spec.ts / guias-hub.spec.ts; here we only need the state to render).
    hub.componentInstance['token'].set({ code: '111222', expiresAt: new Date(Date.now() - 1000).toISOString() });
    hub.detectChanges(); // token expirado

    // Error states (retry-capable list error, generic token error).
    guidesApi['getGuides'].mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    hub.componentInstance.refresh();
    hub.detectChanges();
    guidesApi['getCurrentToken'].mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    hub.componentInstance['loadCurrentToken']('maria-id');
    hub.detectChanges();

    expect(missing()).toHaveLength(0);
  });

  it('AC1 (BR2): renders the 3 seeded guide statuses in the list without a missing key', async () => {
    guidesApi['getGuides'] = vi.fn().mockReturnValue(
      of(
        [
          { id: 'g1', number: 'GU-0001', type: 'CONSULTA', requestingProvider: 'Dr. João', requestedAt: '2026-07-01', status: 'EM_ANALISE' },
          { id: 'g2', number: 'GU-0002', type: 'SP_SADT', requestingProvider: 'Clínica Central', requestedAt: '2026-06-20', status: 'AUTORIZADA' },
          { id: 'g3', number: 'GU-0003', type: 'INTERNACAO', requestingProvider: 'Hospital São Lucas', requestedAt: '2026-06-10', status: 'NEGADA' },
        ],
      ),
    );
    await TestBed.configureTestingModule({
      imports: [GuiasHub],
      providers: [
        provideRouter([]),
        provideI18n(),
        { provide: GuidesApi, useValue: guidesApi },
        { provide: BeneficiarySummaryApi, useValue: summaryApi },
      ],
    }).compileComponents();
    const context = TestBed.inject(BeneficiaryContextService);
    context.accessible.set([{ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' }]);
    const hub = TestBed.createComponent(GuiasHub);
    hub.detectChanges();
    context.active.set({ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' });
    hub.detectChanges();
    expect(missing()).toHaveLength(0);
  });

  it('renders the guide detail — authorized (password/validity), expired-auth notice and denied (reason) — without a missing key', async () => {
    await TestBed.configureTestingModule({
      imports: [GuideDetail],
      providers: [
        provideRouter([]),
        provideI18n(),
        { provide: GuidesApi, useValue: guidesApi },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'guide-1' } } } },
      ],
    }).compileComponents();
    TestBed.inject(BeneficiaryContextService).active.set({
      beneficiaryId: 'maria-id',
      firstName: 'MARIA',
      role: 'TITULAR',
    });
    const detail = TestBed.createComponent(GuideDetail);
    detail.detectChanges();

    guidesApi['getGuide'].mockReturnValue(of({ ...AUTHORIZED_GUIDE, authExpired: true }));
    detail.componentInstance.load();
    detail.detectChanges(); // expired-authorization notice (BR7)

    guidesApi['getGuide'].mockReturnValue(of(DENIED_GUIDE));
    detail.componentInstance.load();
    detail.detectChanges(); // denial reason (BR5)

    guidesApi['getGuide'].mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404, error: { code: 'guide.not-found' } })),
    );
    detail.componentInstance.load();
    detail.detectChanges(); // not-found state

    guidesApi['getGuide'].mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    detail.componentInstance.load();
    detail.detectChanges(); // generic error + retry

    expect(missing()).toHaveLength(0);
  });
});
