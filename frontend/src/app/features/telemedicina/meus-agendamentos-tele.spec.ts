import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { AppointmentView } from '../agendamento/appointments.api';
import { TeleApi } from './tele.api';
import { MeusAgendamentosTele } from './meus-agendamentos-tele';

/** A tele appointment whose slot is `now`, so the join window (10 min before → +30 min) is open. */
function teleAppointment(overrides: Partial<AppointmentView> = {}): AppointmentView {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  const scheduledAt = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
  return {
    id: 'appt-1',
    protocol: 'AG-TELE-1',
    type: 'CONSULTATION',
    specialtyCode: 'CLINICA_GERAL',
    specialtyName: 'Clínica Geral',
    examCode: null,
    examName: null,
    beneficiaryId: 'maria-id',
    beneficiaryName: 'MARIA',
    unitId: 'virtual-tele',
    unitName: 'Telemedicina',
    scheduledAt,
    status: 'AGENDADO',
    telemedicine: true,
    ...overrides,
  };
}

/** A tele appointment far in the future → the join window is closed. */
function futureTeleAppointment(): AppointmentView {
  return teleAppointment({ id: 'appt-future', protocol: 'AG-TELE-2', scheduledAt: '2027-01-01T10:00' });
}

describe('MeusAgendamentosTele (BR14/AC6)', () => {
  let fixture: ComponentFixture<MeusAgendamentosTele>;
  let api: { getTeleAppointments: ReturnType<typeof vi.fn>; joinScheduled: ReturnType<typeof vi.fn> };
  let router: Router;

  const context = {
    accessible: () => [{ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' as const }],
  };

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }
  function build(): void {
    fixture = TestBed.createComponent(MeusAgendamentosTele);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  }

  beforeEach(() => {
    api = {
      getTeleAppointments: vi.fn().mockReturnValue(of({ upcoming: [teleAppointment()], history: [] })),
      joinScheduled: vi.fn().mockReturnValue(of(undefined)),
    };
    TestBed.configureTestingModule({
      imports: [MeusAgendamentosTele],
      providers: [
        provideI18n(),
        { provide: TeleApi, useValue: api },
        { provide: BeneficiaryContextService, useValue: context },
      ],
    });
  });

  it('loads only telemedicine appointments (server-scoped) and shows the Telemedicina badge', () => {
    build();
    expect(api.getTeleAppointments).toHaveBeenCalled();
    expect(el().querySelector('[data-testid="meus-tele-card-appt-1"]')).not.toBeNull();
    expect(el().querySelector('[data-testid="meus-tele-card-badge"]')?.textContent).toContain('Telemedicina');
  });

  it('AC6: "Entrar na consulta" is ENABLED inside the join window', () => {
    build();
    const button = el().querySelector('[data-testid="meus-tele-entrar-appt-1"]') as HTMLButtonElement;
    expect(button.disabled).toBe(false);
  });

  it('AC6: "Entrar na consulta" is DISABLED outside the window, showing the window hint', () => {
    api.getTeleAppointments.mockReturnValue(of({ upcoming: [futureTeleAppointment()], history: [] }));
    build();
    const button = el().querySelector('[data-testid="meus-tele-entrar-appt-future"]') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    expect(el().querySelector('[data-testid="meus-tele-janela-appt-future"]')).not.toBeNull();
  });

  it('BR14: joining opens the live room', () => {
    build();
    (el().querySelector('[data-testid="meus-tele-entrar-appt-1"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(api.joinScheduled).toHaveBeenCalledWith('appt-1');
    expect(router.navigate).toHaveBeenCalledWith(['/telemedicina/sessao']);
  });

  it('surfaces a 409 tele.join-window-closed inline (window race)', () => {
    api.joinScheduled.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'tele.join-window-closed' }, status: 409 })),
    );
    build();
    (el().querySelector('[data-testid="meus-tele-entrar-appt-1"]') as HTMLElement).click();
    fixture.detectChanges();
    expect(el().querySelector('[data-testid="meus-tele-entrar-erro-appt-1"]')).not.toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('filtering by beneficiary reloads the list', () => {
    build();
    fixture.componentInstance.onFilterChange('maria-id');
    expect(api.getTeleAppointments).toHaveBeenLastCalledWith('maria-id');
  });
});
