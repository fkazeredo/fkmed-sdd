import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { TeleApi, TeleCatalog } from './tele.api';
import { ProntoAtendimento } from './pronto-atendimento';

const CATALOG: TeleCatalog = {
  symptoms: [
    { code: 'CEFALEIA', name: 'Dor de cabeça' },
    { code: 'FEBRE', name: 'Febre' },
    { code: 'DOR_TORACICA', name: 'Dor no peito', emergency: true },
  ],
  term: { version: '1.0', body: 'Termo de teleatendimento — versão de teste.' },
};

const VALID_COMPLAINT = 'Dor de cabeça persistente há dois dias.';

describe('ProntoAtendimento (BR2/BR3/BR4/BR5/BR7)', () => {
  let fixture: ComponentFixture<ProntoAtendimento>;
  let api: {
    getCurrentSession: ReturnType<typeof vi.fn>;
    getCatalog: ReturnType<typeof vi.fn>;
    createSession: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  const context = {
    active: () => ({ beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' as const }),
  };

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }
  function click(testid: string): void {
    (el().querySelector(`[data-testid="${testid}"]`) as HTMLElement).click();
    fixture.detectChanges();
  }
  function fillValidTriage(): void {
    fixture.componentInstance['complaint'].set(VALID_COMPLAINT);
    fixture.componentInstance.selectDuration('D1_3');
    fixture.detectChanges();
  }

  beforeEach(async () => {
    api = {
      // No active session on entry (404) → the triage catalog loads.
      getCurrentSession: vi.fn().mockReturnValue(
        throwError(() => new HttpErrorResponse({ error: { code: 'tele.session-not-found' }, status: 404 })),
      ),
      getCatalog: vi.fn().mockReturnValue(of(CATALOG)),
      createSession: vi.fn().mockReturnValue(of({ state: 'EM_FILA', position: 4, etaMinutes: 12 })),
    };
    await TestBed.configureTestingModule({
      imports: [ProntoAtendimento],
      providers: [
        provideI18n(),
        { provide: TeleApi, useValue: api },
        { provide: BeneficiaryContextService, useValue: context },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProntoAtendimento);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('loads the catalog and shows the attended beneficiary (BR2/BR13)', () => {
    expect(api.getCatalog).toHaveBeenCalled();
    expect(el().querySelector('[data-testid="triagem-beneficiario"]')?.textContent).toContain('PEDRO');
  });

  it('AC5: redirects to the live session when one is already active, without re-triaging (BR7)', () => {
    // The beforeEach fixture already loaded the catalog (no session); reset and simulate an active one.
    api.getCurrentSession.mockReturnValue(of({ state: 'EM_FILA', position: 2, etaMinutes: 6 }));
    api.getCatalog.mockClear();
    (router.navigate as unknown as ReturnType<typeof vi.fn>).mockClear();
    const fresh = TestBed.createComponent(ProntoAtendimento);
    fresh.detectChanges();
    expect(router.navigate).toHaveBeenCalledWith(['/telemedicina/sessao']);
    expect(api.getCatalog).not.toHaveBeenCalled();
  });

  it('BR2: shows the live complaint count and blocks advancing when it is too short', () => {
    fixture.componentInstance['complaint'].set('curta'); // 5 chars
    fixture.componentInstance.selectDuration('HORAS');
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="triagem-queixa-contador"]')?.textContent).toContain('5/500');
    expect(el().querySelector('[data-testid="triagem-queixa-validacao"]')).not.toBeNull();
    click('triagem-avancar');
    expect(el().querySelector('[data-testid="triagem-gate"]')).not.toBeNull();
    expect(fixture.componentInstance['step']()).toBe('triagem');
  });

  it('BR2: blocks advancing without a chosen duration', () => {
    fixture.componentInstance['complaint'].set(VALID_COMPLAINT);
    fixture.detectChanges();
    click('triagem-avancar');
    expect(el().querySelector('[data-testid="triagem-gate"]')).not.toBeNull();
    expect(fixture.componentInstance['step']()).toBe('triagem');
  });

  it('BR3: selecting an emergency symptom raises the ER alert and blocks advancing until acknowledged', () => {
    fillValidTriage();
    fixture.componentInstance.toggleSymptom('DOR_TORACICA');
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="triagem-emergencia-alerta"]')).not.toBeNull();

    click('triagem-avancar');
    expect(fixture.componentInstance['step']()).toBe('triagem'); // still blocked
    expect(el().querySelector('[data-testid="triagem-gate"]')).not.toBeNull();

    click('triagem-emergencia-proceder');
    click('triagem-avancar');
    expect(fixture.componentInstance['step']()).toBe('termo'); // proceeds at user's decision
  });

  it('BR3: the ER button routes to the 24h network (SPEC-0008)', () => {
    fillValidTriage();
    fixture.componentInstance.toggleSymptom('DOR_TORACICA');
    fixture.detectChanges();
    click('triagem-emergencia-rede');
    expect(router.navigate).toHaveBeenCalledWith(['/rede/busca']);
  });

  it('deselecting the emergency symptom clears the acknowledgment', () => {
    fillValidTriage();
    fixture.componentInstance.toggleSymptom('DOR_TORACICA');
    fixture.componentInstance.acknowledgeEmergency();
    fixture.componentInstance.toggleSymptom('DOR_TORACICA');
    expect(fixture.componentInstance['emergencyAcknowledged']()).toBe(false);
  });

  it('BR4: advancing shows the versioned term and the queue button is disabled until accepted', () => {
    fillValidTriage();
    click('triagem-avancar');
    expect(el().querySelector('[data-testid="termo-corpo"]')?.textContent).toContain('Termo de teleatendimento');
    expect(el().textContent).toContain('1.0');
    const enter = el().querySelector('[data-testid="termo-entrar-fila"]') as HTMLButtonElement;
    expect(enter.disabled).toBe(true);
  });

  it('BR5/BR7: accepting the term POSTs the frozen body and routes to the live session', () => {
    fillValidTriage();
    fixture.componentInstance.toggleSymptom('CEFALEIA');
    fixture.componentInstance['otherSymptom'].set('tontura');
    fixture.detectChanges();
    click('triagem-avancar');
    fixture.componentInstance['termAccepted'].set(true);
    fixture.detectChanges();
    click('termo-entrar-fila');

    expect(api.createSession).toHaveBeenCalledWith({
      beneficiaryId: 'pedro-id',
      complaint: VALID_COMPLAINT,
      symptoms: ['CEFALEIA'],
      otherSymptom: 'tontura',
      duration: 'D1_3',
      termVersion: '1.0',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/telemedicina/sessao']);
  });

  it('maps 422 tele.complaint-invalid back to the triage step', () => {
    api.createSession.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'tele.complaint-invalid' }, status: 422 })),
    );
    fillValidTriage();
    click('triagem-avancar');
    fixture.componentInstance['termAccepted'].set(true);
    fixture.detectChanges();
    click('termo-entrar-fila');
    expect(fixture.componentInstance['step']()).toBe('triagem');
    expect(el().querySelector('[data-testid="triagem-gate"]')).not.toBeNull();
  });

  it('maps 422 tele.term-not-accepted to an inline term error', () => {
    api.createSession.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'tele.term-not-accepted' }, status: 422 })),
    );
    fillValidTriage();
    click('triagem-avancar');
    fixture.componentInstance['termAccepted'].set(true);
    fixture.detectChanges();
    click('termo-entrar-fila');
    expect(el().querySelector('[data-testid="termo-erro"]')?.textContent).toContain('aceit');
  });
});
