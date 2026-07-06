import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { Appointment, AppointmentsApi, AvailabilityDay } from './appointments.api';
import { MeusAgendamentos } from './meus-agendamentos';

const DAYS: AvailabilityDay[] = [
  { date: '2026-07-20', slots: [{ slot: '2026-07-20T11:00', remaining: 3, available: true }] },
];

const APPOINTMENTS: Appointment[] = [
  {
    id: 'a-late',
    type: 'CONSULTATION',
    specialty: 'Cardiologia',
    exam: null,
    beneficiary: 'PEDRO',
    unit: 'Unidade Centro',
    scheduledAt: '2026-07-15T09:00',
    status: 'AGENDADO',
    protocol: 'AG-20260704-0002',
    unitId: 'u1',
    specialtyCode: 'CARDIOLOGIA',
  },
  {
    id: 'a-soon',
    type: 'EXAM',
    specialty: null,
    exam: 'Hemograma',
    beneficiary: 'MARIA',
    unit: 'Unidade Tijuca',
    scheduledAt: '2026-07-10T08:00',
    status: 'REAGENDADO',
    protocol: 'AG-20260704-0001',
    unitId: 'u2',
    examCode: 'HEMOGRAMA',
  },
  {
    id: 'a-cancel',
    type: 'CONSULTATION',
    specialty: 'Dermatologia',
    exam: null,
    beneficiary: 'MARIA',
    unit: 'Unidade Centro',
    scheduledAt: '2026-06-20T10:00',
    status: 'CANCELADO',
    protocol: 'AG-20260601-0003',
  },
  {
    id: 'a-done',
    type: 'CONSULTATION',
    specialty: 'Ortopedia',
    exam: null,
    beneficiary: 'PEDRO',
    unit: 'Unidade Centro',
    scheduledAt: '2026-06-25T14:00',
    status: 'REALIZADO',
    protocol: 'AG-20260601-0004',
  },
];

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
      getAppointments: vi.fn().mockReturnValue(of(APPOINTMENTS)),
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

  it('BR13: Próximos shows only active items soonest-first', () => {
    // a-soon (07-10) before a-late (07-15); cancelled/done excluded.
    expect(cardIds()).toEqual(['meus-card-a-soon', 'meus-card-a-late']);
  });

  it('BR13: Histórico shows finalized items most-recent-first', () => {
    (el().querySelector('[data-testid="meus-tab-historico"]') as HTMLElement).click();
    fixture.detectChanges();
    // a-done (06-25) before a-cancel (06-20).
    expect(cardIds()).toEqual(['meus-card-a-done', 'meus-card-a-cancel']);
  });

  it('shows the type, subject, beneficiary, unit and status on a card', () => {
    const card = el().querySelector('[data-testid="meus-card-a-soon"]') as HTMLElement;
    expect(card.querySelector('[data-testid="meus-card-assunto"]')?.textContent).toContain('Hemograma');
    expect(card.querySelector('[data-testid="meus-card-beneficiario"]')?.textContent).toContain('MARIA');
    expect(card.querySelector('[data-testid="meus-card-unidade"]')?.textContent).toContain('Unidade Tijuca');
    expect(card.querySelector('[data-testid="meus-card-quando"]')?.textContent).toContain('08:00');
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
    expect(el().querySelector('[data-testid="meus-cancelar-a-cancel"]')).toBeNull();
  });

  it('BR9: cancelling with a reason calls the API and reloads', () => {
    const soon = APPOINTMENTS[1];
    fixture.componentInstance.askCancel(soon);
    fixture.componentInstance['cancelReason'].set('Imprevisto');
    api.getAppointments.mockClear();
    fixture.componentInstance.confirmCancel();
    expect(api.cancel).toHaveBeenCalledWith('a-soon', 'Imprevisto');
    expect(api.getAppointments).toHaveBeenCalledTimes(1);
  });

  it('BR9: an empty reason sends undefined (optional)', () => {
    fixture.componentInstance.askCancel(APPOINTMENTS[1]);
    fixture.componentInstance['cancelReason'].set('   ');
    fixture.componentInstance.confirmCancel();
    expect(api.cancel).toHaveBeenCalledWith('a-soon', undefined);
  });

  it('BR9: maps cancel-too-late to an inline dialog message', () => {
    api.cancel.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'appointment.cancel-too-late' }, status: 409 })),
    );
    fixture.componentInstance.askCancel(APPOINTMENTS[1]);
    fixture.componentInstance.confirmCancel();
    expect(fixture.componentInstance['cancelError']()).toBe('appointment.cancel-too-late');
  });

  it('BR10: opening reschedule loads availability for the kept unit + scope', () => {
    fixture.componentInstance.askReschedule(APPOINTMENTS[1]); // exam item
    expect(api.getAvailability).toHaveBeenCalledWith({ unitId: 'u2', specialty: undefined, exam: 'HEMOGRAMA' });
  });

  it('BR10: confirming reschedule posts the new slot (same protocol kept server-side) and reloads', () => {
    fixture.componentInstance.askReschedule(APPOINTMENTS[0]);
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
    fixture.componentInstance.askReschedule(APPOINTMENTS[0]);
    fixture.componentInstance.selectRescheduleSlot('2026-07-20T11:00');
    api.getAvailability.mockClear();
    fixture.componentInstance.confirmReschedule();
    expect(fixture.componentInstance['rescheduleTarget']()).not.toBeNull();
    expect(fixture.componentInstance['rescheduleError']()).toBe('appointment.slot-taken');
    expect(fixture.componentInstance['rescheduleSlot']()).toBeNull();
    expect(api.getAvailability).toHaveBeenCalledTimes(1);
  });

  it('renders the future Telemedicina badge when flagged (BR13)', () => {
    api.getAppointments.mockReturnValue(
      of([{ ...APPOINTMENTS[1], telemedicine: true }]),
    );
    fixture.componentInstance.load();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="meus-card-telemedicina"]')).not.toBeNull();
  });

  it('shows the empty state when there are no commitments in the tab', () => {
    api.getAppointments.mockReturnValue(of([]));
    fixture.componentInstance.load();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="meus-vazio"]')).not.toBeNull();
  });
});
