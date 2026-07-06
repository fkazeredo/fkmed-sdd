import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { AppointmentList, AppointmentsApi, AppointmentView, AvailabilityDay } from './appointments.api';
import { MeusAgendamentos } from './meus-agendamentos';

const DAYS: AvailabilityDay[] = [
  { date: '2026-07-20', slots: [{ slot: '2026-07-20T11:00', remaining: 3, available: true }] },
];

// The backend already splits + orders: upcoming soonest-first, history most-recent-first.
const UPCOMING: AppointmentView[] = [
  {
    id: 'a-soon', protocol: 'AG-20260704-0001', type: 'EXAM', specialtyCode: null, examCode: 'HEMOGRAMA',
    examName: 'Hemograma', beneficiaryId: 'maria-id', beneficiaryName: 'MARIA', unitId: 'u2',
    unitName: 'Unidade Tijuca', scheduledAt: '2026-07-10T08:00', status: 'REAGENDADO',
  },
  {
    id: 'a-late', protocol: 'AG-20260704-0002', type: 'CONSULTATION', specialtyCode: 'CARDIOLOGIA',
    specialtyName: 'Cardiologia', examCode: null, examName: null, beneficiaryId: 'pedro-id',
    beneficiaryName: 'PEDRO', unitId: 'u1', unitName: 'Unidade Centro', scheduledAt: '2026-07-15T09:00',
    status: 'AGENDADO',
  },
];
const HISTORY: AppointmentView[] = [
  {
    id: 'a-done', protocol: 'AG-20260601-0004', type: 'CONSULTATION', specialtyCode: 'ORTOPEDIA',
    specialtyName: 'Ortopedia', examCode: null, examName: null, beneficiaryId: 'pedro-id',
    beneficiaryName: 'PEDRO', unitId: 'u1', unitName: 'Unidade Centro', scheduledAt: '2026-06-25T14:00',
    status: 'REALIZADO',
  },
  {
    id: 'a-cancel', protocol: 'AG-20260601-0003', type: 'CONSULTATION', specialtyCode: 'DERMATOLOGIA',
    specialtyName: 'Dermatologia', examCode: null, examName: null, beneficiaryId: 'maria-id',
    beneficiaryName: 'MARIA', unitId: 'u1', unitName: 'Unidade Centro', scheduledAt: '2026-06-20T10:00',
    status: 'CANCELADO',
  },
];
const LIST: AppointmentList = { upcoming: UPCOMING, history: HISTORY };

describe('MeusAgendamentos (BR13/BR9/BR10)', () => {
  let fixture: ComponentFixture<MeusAgendamentos>;
  let api: {
    getAppointments: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
    reschedule: ReturnType<typeof vi.fn>;
    getAvailability: ReturnType<typeof vi.fn>;
  };

  const context = {
    accessible: () => [
      { beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' as const },
      { beneficiaryId: 'pedro-id', firstName: 'PEDRO', role: 'DEPENDENT' as const },
    ],
    active: () => ({ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' as const }),
  };

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }
  function cardIds(): string[] {
    return Array.from(el().querySelectorAll('li[data-testid^="meus-card-a-"]')).map((node) =>
      node.getAttribute('data-testid'),
    ) as string[];
  }

  beforeEach(async () => {
    api = {
      getAppointments: vi.fn().mockReturnValue(of(LIST)),
      cancel: vi.fn().mockReturnValue(of(null)),
      reschedule: vi.fn().mockReturnValue(of({ protocol: 'AG-20260704-0001', status: 'REAGENDADO' })),
      getAvailability: vi.fn().mockReturnValue(of(DAYS)),
    };
    await TestBed.configureTestingModule({
      imports: [MeusAgendamentos],
      providers: [
        provideI18n(),
        { provide: AppointmentsApi, useValue: api },
        { provide: BeneficiaryContextService, useValue: context },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(MeusAgendamentos);
    fixture.detectChanges();
  });

  it('loads all accessible commitments on init (no beneficiaryId)', () => {
    expect(api.getAppointments).toHaveBeenCalledWith(undefined);
  });

  it('BR13: Próximos renders the server-ordered upcoming list as-is (no client re-sort)', () => {
    expect(cardIds()).toEqual(['meus-card-a-soon', 'meus-card-a-late']);
  });

  it('BR13: Histórico renders the server-ordered history list as-is', () => {
    (el().querySelector('[data-testid="meus-tab-historico"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(cardIds()).toEqual(['meus-card-a-done', 'meus-card-a-cancel']);
  });

  it('shows the exam name, beneficiary and unit names on a card', () => {
    const card = el().querySelector('[data-testid="meus-card-a-soon"]') as HTMLElement;
    expect(card.querySelector('[data-testid="meus-card-assunto"]')?.textContent).toContain('Hemograma');
    expect(card.querySelector('[data-testid="meus-card-beneficiario"]')?.textContent).toContain('MARIA');
    expect(card.querySelector('[data-testid="meus-card-unidade"]')?.textContent).toContain('Unidade Tijuca');
    expect(card.querySelector('[data-testid="meus-card-quando"]')?.textContent).toContain('08:00');
  });

  it('uses the specialty name for a consultation subject', () => {
    const card = el().querySelector('[data-testid="meus-card-a-late"]') as HTMLElement;
    expect(card.querySelector('[data-testid="meus-card-assunto"]')?.textContent).toContain('Cardiologia');
  });

  it('BR13: filtering by beneficiary re-queries with the beneficiaryId', () => {
    fixture.componentInstance.onFilterChange('pedro-id');
    expect(api.getAppointments).toHaveBeenLastCalledWith('pedro-id');
    fixture.componentInstance.onFilterChange('');
    expect(api.getAppointments).toHaveBeenLastCalledWith(undefined);
  });

  it('only active items expose cancel/reschedule (BR9/BR10)', () => {
    expect(el().querySelector('[data-testid="meus-cancelar-a-soon"]')).not.toBeNull();
    expect(el().querySelector('[data-testid="meus-reagendar-a-soon"]')).not.toBeNull();
    (el().querySelector('[data-testid="meus-tab-historico"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="meus-cancelar-a-done"]')).toBeNull();
  });

  it('BR9: cancelling with a reason calls the API and reloads', () => {
    fixture.componentInstance.askCancel(UPCOMING[0]);
    fixture.componentInstance['cancelReason'].set('Imprevisto');
    api.getAppointments.mockClear();
    fixture.componentInstance.confirmCancel();
    expect(api.cancel).toHaveBeenCalledWith('a-soon', 'Imprevisto');
    expect(api.getAppointments).toHaveBeenCalledTimes(1);
  });

  it('BR9: an empty reason sends undefined (optional)', () => {
    fixture.componentInstance.askCancel(UPCOMING[0]);
    fixture.componentInstance['cancelReason'].set('   ');
    fixture.componentInstance.confirmCancel();
    expect(api.cancel).toHaveBeenCalledWith('a-soon', undefined);
  });

  it('BR9: maps cancel-too-late to an inline dialog message', () => {
    api.cancel.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'appointment.cancel-too-late' }, status: 409 })),
    );
    fixture.componentInstance.askCancel(UPCOMING[0]);
    fixture.componentInstance.confirmCancel();
    expect(fixture.componentInstance['cancelError']()).toBe('appointment.cancel-too-late');
  });

  it('maps a 404 appointment.not-found on cancel to its inline message', () => {
    api.cancel.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'appointment.not-found' }, status: 404 })),
    );
    fixture.componentInstance.askCancel(UPCOMING[0]);
    fixture.componentInstance.confirmCancel();
    expect(fixture.componentInstance['cancelError']()).toBe('appointment.not-found');
  });

  it('BR10: opening reschedule loads availability for the kept unit + exam scope', () => {
    fixture.componentInstance.askReschedule(UPCOMING[0]); // exam item
    expect(api.getAvailability).toHaveBeenCalledWith({ unitId: 'u2', specialty: undefined, exam: 'HEMOGRAMA' });
  });

  it('BR10: reschedule of a consultation sends the specialty scope', () => {
    fixture.componentInstance.askReschedule(UPCOMING[1]); // consultation item
    expect(api.getAvailability).toHaveBeenCalledWith({ unitId: 'u1', specialty: 'CARDIOLOGIA', exam: undefined });
  });

  it('BR10: confirming reschedule posts the new slot (same protocol kept server-side) and reloads', () => {
    fixture.componentInstance.askReschedule(UPCOMING[1]);
    fixture.componentInstance.selectRescheduleSlot('2026-07-20T11:00');
    api.getAppointments.mockClear();
    fixture.componentInstance.confirmReschedule();
    expect(api.reschedule).toHaveBeenCalledWith('a-late', '2026-07-20T11:00');
    expect(api.getAppointments).toHaveBeenCalledTimes(1);
  });

  it('BR6: a slot-taken race on reschedule keeps the dialog open and reloads availability', () => {
    api.reschedule.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'appointment.slot-taken' }, status: 409 })),
    );
    fixture.componentInstance.askReschedule(UPCOMING[1]);
    fixture.componentInstance.selectRescheduleSlot('2026-07-20T11:00');
    api.getAvailability.mockClear();
    fixture.componentInstance.confirmReschedule();
    expect(fixture.componentInstance['rescheduleTarget']()).not.toBeNull();
    expect(fixture.componentInstance['rescheduleError']()).toBe('appointment.slot-taken');
    expect(fixture.componentInstance['rescheduleSlot']()).toBeNull();
    expect(api.getAvailability).toHaveBeenCalledTimes(1);
  });

  it('maps a 404 appointment.not-found on reschedule to its inline message', () => {
    api.reschedule.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'appointment.not-found' }, status: 404 })),
    );
    fixture.componentInstance.askReschedule(UPCOMING[1]);
    fixture.componentInstance.selectRescheduleSlot('2026-07-20T11:00');
    fixture.componentInstance.confirmReschedule();
    expect(fixture.componentInstance['rescheduleError']()).toBe('appointment.not-found');
  });

  it('renders the future Telemedicina badge when flagged (BR13)', () => {
    api.getAppointments.mockReturnValue(of({ upcoming: [{ ...UPCOMING[0], telemedicine: true }], history: [] }));
    fixture.componentInstance.load();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="meus-card-telemedicina"]')).not.toBeNull();
  });

  it('shows the empty state when there are no commitments in the tab', () => {
    api.getAppointments.mockReturnValue(of({ upcoming: [], history: [] }));
    fixture.componentInstance.load();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="meus-vazio"]')).not.toBeNull();
  });
});
