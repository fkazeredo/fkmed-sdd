import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MissingTranslationHandler } from '@ngx-translate/core';
import { of, Subject, throwError } from 'rxjs';
import { provideI18n, ReportMissingTranslationHandler } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { AppointmentView, AvailabilityDay, RegistryOption } from '../agendamento/appointments.api';
import { TeleApi, TeleCatalog, TeleSession } from './tele.api';
import { TeleSessionStreamService } from './tele-session-stream.service';
import { TelemedicinaHub } from './telemedicina-hub';
import { TeleInstabilityBanner } from './tele-instability-banner';
import { ProntoAtendimento } from './pronto-atendimento';
import { SessaoAtendimento } from './sessao-atendimento';
import { AgendarTeleconsulta } from './agendar-teleconsulta';
import { MeusAgendamentosTele } from './meus-agendamentos-tele';

/**
 * SPEC-0010: every visible telemedicine UI string resolves from the pt-BR bundle. Renders every
 * screen of the slice through all its states/branches with a recording MissingTranslationHandler;
 * a single missing key fails. Kept inside the feature (the central i18n-completeness spec is core
 * scope) — the architect may fold these into it at integration.
 */
describe('telemedicina i18n completeness (pt-BR)', () => {
  const CATALOG: TeleCatalog = {
    symptoms: [
      { code: 'CEFALEIA', name: 'Dor de cabeça' },
      { code: 'DOR_TORACICA', name: 'Dor no peito', emergency: true },
    ],
    term: { version: '1.0', body: 'Termo de teleatendimento.' },
  };
  const SPECIALTIES: RegistryOption[] = [{ code: 'CLINICA_GERAL', name: 'Clínica Geral' }];
  const DAYS: AvailabilityDay[] = [
    { date: '2026-07-10', slots: [{ slot: '2026-07-10T09:00', remaining: 2, available: true }] },
  ];
  const TELE_APPT: AppointmentView = {
    id: 'a1', protocol: 'AG-TELE-1', type: 'CONSULTATION', specialtyCode: 'CLINICA_GERAL',
    specialtyName: 'Clínica Geral', examCode: null, examName: null, beneficiaryId: 'maria-id',
    beneficiaryName: 'MARIA', unitId: 'virtual', unitName: 'Telemedicina',
    scheduledAt: '2027-01-01T10:00', status: 'AGENDADO', telemedicine: true,
  };

  let api: Record<string, ReturnType<typeof vi.fn>>;
  let stream$: Subject<TeleSession>;

  beforeEach(async () => {
    stream$ = new Subject<TeleSession>();
    api = {
      getActiveInstabilityNotice: vi.fn().mockReturnValue(
        of({ title: 'Instabilidade momentânea da Telemedicina', body: 'Estamos normalizando.' }),
      ),
      getCurrentSession: vi.fn().mockReturnValue(
        throwError(() => new HttpErrorResponse({ error: { code: 'tele.session-not-found' }, status: 404 })),
      ),
      getCatalog: vi.fn().mockReturnValue(of(CATALOG)),
      createSession: vi.fn().mockReturnValue(
        throwError(() => new HttpErrorResponse({ error: { code: 'tele.term-not-accepted' }, status: 422 })),
      ),
      leaveSession: vi.fn().mockReturnValue(of(undefined)),
      getTeleSpecialties: vi.fn().mockReturnValue(of(SPECIALTIES)),
      getTeleAvailability: vi.fn().mockReturnValue(of(DAYS)),
      bookTeleConsultation: vi.fn().mockReturnValue(of({ protocol: 'AG-TELE-9', status: 'AGENDADO' })),
      getTeleAppointments: vi.fn().mockReturnValue(of({ upcoming: [TELE_APPT], history: [] })),
      joinScheduled: vi.fn().mockReturnValue(
        throwError(() => new HttpErrorResponse({ error: { code: 'tele.join-window-closed' }, status: 409 })),
      ),
    };
    await TestBed.configureTestingModule({
      imports: [TelemedicinaHub, TeleInstabilityBanner, ProntoAtendimento, SessaoAtendimento, AgendarTeleconsulta, MeusAgendamentosTele],
      providers: [
        provideRouter([]),
        provideI18n(),
        { provide: TeleApi, useValue: api },
        { provide: TeleSessionStreamService, useValue: { connect: () => stream$.asObservable() } },
        {
          provide: BeneficiaryContextService,
          useValue: {
            active: () => ({ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' }),
            accessible: () => [{ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' }],
          },
        },
      ],
    }).compileComponents();
  });

  function missing(): string[] {
    return Array.from((TestBed.inject(MissingTranslationHandler) as ReportMissingTranslationHandler).missing);
  }

  it('renders the hub + instability banner without a missing key (BR1/AC7)', () => {
    const hub = TestBed.createComponent(TelemedicinaHub);
    hub.detectChanges();
    expect(missing()).toHaveLength(0);
  });

  it('renders Pronto Atendimento — triage, emergency alert, gates and term — without a missing key', () => {
    const pronto = TestBed.createComponent(ProntoAtendimento);
    pronto.detectChanges();
    const c = pronto.componentInstance;
    // Gate messages.
    c['complaint'].set('x');
    pronto.detectChanges();
    c.goToTerm(); // gate.queixa
    c['complaint'].set('Dor persistente há dois dias.');
    pronto.detectChanges();
    c.goToTerm(); // gate.duracao
    c.selectDuration('D1_3');
    c.toggleSymptom('DOR_TORACICA'); // emergency alert
    pronto.detectChanges();
    c.goToTerm(); // gate.emergencia
    pronto.detectChanges();
    c.acknowledgeEmergency();
    pronto.detectChanges();
    c.goToTerm(); // -> termo
    c['termAccepted'].set(true);
    pronto.detectChanges();
    c.enterQueue(); // 422 term-not-accepted → termo-erro
    pronto.detectChanges();
    expect(missing()).toHaveLength(0);
  });

  it('renders every session state — queue, room, summary, abandoned, empty, leave — without a missing key', () => {
    const sessao = TestBed.createComponent(SessaoAtendimento);
    sessao.detectChanges(); // conectando (no emit yet)

    stream$.next({ state: 'EM_FILA', position: 3, etaMinutes: 8 });
    sessao.detectChanges();
    sessao.componentInstance.askLeave(); // sair confirm block
    sessao.detectChanges();

    stream$.next({
      state: 'EM_ATENDIMENTO',
      professional: { name: 'Dra. Ana', crm: 'CRM-RJ 1' },
      room: { startedAt: '2026-07-06T15:00' },
    });
    sessao.detectChanges();

    stream$.next({
      state: 'ENCERRADA',
      professional: { name: 'Dra. Ana', crm: 'CRM-RJ 1' },
      room: { durationMinutes: 12, guidance: 'Repouso.', documents: [{ id: 'd1', type: 'PRESCRIPTION', description: 'Receita' }] },
    });
    sessao.detectChanges();

    stream$.next({ state: 'ENCERRADA', room: { documents: [] } }); // semDocumentos branch
    sessao.detectChanges();

    stream$.next({ state: 'ABANDONADA' });
    sessao.detectChanges();

    // Empty state: the stream errors 404 (no active session) → semSessao / voltarHub.
    stream$.error(new HttpErrorResponse({ status: 404 }));
    sessao.detectChanges();
    expect(missing()).toHaveLength(0);
  });

  it('renders the scheduling wizard — all steps, errors and success — without a missing key (BR14)', () => {
    const agendar = TestBed.createComponent(AgendarTeleconsulta);
    agendar.detectChanges();
    const c = agendar.componentInstance;
    c.next(); // gate.especialidade
    agendar.detectChanges();
    c.selectSpecialty('CLINICA_GERAL');
    agendar.detectChanges();
    c.next(); // -> slot
    c['errorKey'].set('appointment.slot-taken'); // slot-taken banner
    agendar.detectChanges();
    c.selectSlot('2026-07-10T09:00');
    agendar.detectChanges();
    c.next(); // -> review
    c['errorKey'].set('appointment.time-conflict');
    agendar.detectChanges();
    c['errorKey'].set(null);
    agendar.detectChanges();
    c.confirm(); // -> success
    agendar.detectChanges();
    expect(missing()).toHaveLength(0);
  });

  it('renders the tele appointments list — join window closed, error and empty — without a missing key (BR14/AC6)', () => {
    const meus = TestBed.createComponent(MeusAgendamentosTele);
    meus.detectChanges(); // list with a closed-window appointment + janela hint
    meus.componentInstance.join(TELE_APPT); // 409 join-window-closed → inline error
    meus.detectChanges();
    meus.componentInstance.setTab('historico'); // empty tab
    meus.detectChanges();
    // Error + retry state.
    api['getTeleAppointments'].mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );
    meus.componentInstance.load();
    meus.detectChanges();
    expect(missing()).toHaveLength(0);
  });
});
