import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { AvailabilityDay, RegistryOption } from '../agendamento/appointments.api';
import { TeleApi } from './tele.api';
import { AgendarTeleconsulta } from './agendar-teleconsulta';

const SPECIALTIES: RegistryOption[] = [
  { code: 'CLINICA_GERAL', name: 'Clínica Geral' },
  { code: 'DERMATOLOGIA', name: 'Dermatologia' },
];
const DAYS: AvailabilityDay[] = [
  { date: '2026-07-10', slots: [{ slot: '2026-07-10T09:00', remaining: 2, available: true }] },
];

describe('AgendarTeleconsulta (BR14)', () => {
  let fixture: ComponentFixture<AgendarTeleconsulta>;
  let api: {
    getTeleSpecialties: ReturnType<typeof vi.fn>;
    getTeleAvailability: ReturnType<typeof vi.fn>;
    bookTeleConsultation: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  const context = {
    active: () => ({ beneficiaryId: 'maria-id', firstName: 'MARIA', role: 'TITULAR' as const }),
  };

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }
  function click(testid: string): void {
    (el().querySelector(`[data-testid="${testid}"]`) as HTMLElement).click();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    api = {
      getTeleSpecialties: vi.fn().mockReturnValue(of(SPECIALTIES)),
      getTeleAvailability: vi.fn().mockReturnValue(of(DAYS)),
      bookTeleConsultation: vi.fn().mockReturnValue(of({ protocol: 'AG-20260706-0007', status: 'AGENDADO' })),
    };
    await TestBed.configureTestingModule({
      imports: [AgendarTeleconsulta],
      providers: [
        provideI18n(),
        { provide: TeleApi, useValue: api },
        { provide: BeneficiaryContextService, useValue: context },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AgendarTeleconsulta);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  it('loads tele specialties on init', () => {
    expect(api.getTeleSpecialties).toHaveBeenCalled();
    expect(el().querySelector('[data-testid="agendar-teleconsulta-page"]')).not.toBeNull();
  });

  it('BR14: blocks advancing without a specialty and does not load the agenda', () => {
    click('agendar-proximo');
    expect(el().querySelector('[data-testid="agendar-gate"]')).not.toBeNull();
    expect(api.getTeleAvailability).not.toHaveBeenCalled();
  });

  it('BR14/DL-0018: completes specialty → slot → review → confirm with the active beneficiary', () => {
    fixture.componentInstance.selectSpecialty('CLINICA_GERAL');
    fixture.detectChanges();
    click('agendar-proximo'); // -> slot
    expect(api.getTeleAvailability).toHaveBeenCalledWith('CLINICA_GERAL');
    fixture.componentInstance.selectSlot('2026-07-10T09:00');
    fixture.detectChanges();
    click('agendar-proximo'); // -> review

    expect(el().querySelector('[data-testid="agendar-revisao-beneficiario"]')?.textContent).toContain('MARIA');
    expect(el().querySelector('[data-testid="agendar-revisao-especialidade"]')?.textContent).toContain('Clínica Geral');
    expect(el().querySelector('[data-testid="agendar-revisao-modalidade"]')?.textContent).toContain('Telemedicina');

    click('agendar-confirmar');
    expect(api.bookTeleConsultation).toHaveBeenCalledWith({
      beneficiaryId: 'maria-id',
      specialty: 'CLINICA_GERAL',
      slot: '2026-07-10T09:00',
    });
    expect(el().querySelector('[data-testid="agendar-protocolo"]')?.textContent).toContain('AG-20260706-0007');
  });

  it('BR6 (inherited): a slot-taken race returns to the time step with a fresh agenda', () => {
    api.bookTeleConsultation.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'appointment.slot-taken' }, status: 409 })),
    );
    fixture.componentInstance.selectSpecialty('CLINICA_GERAL');
    fixture.detectChanges();
    click('agendar-proximo');
    fixture.componentInstance.selectSlot('2026-07-10T09:00');
    fixture.detectChanges();
    click('agendar-proximo');
    api.getTeleAvailability.mockClear();
    click('agendar-confirmar');
    expect(fixture.componentInstance['step']()).toBe('slot');
    expect(el().querySelector('[data-testid="agendar-slot-taken"]')).not.toBeNull();
    expect(api.getTeleAvailability).toHaveBeenCalledTimes(1);
  });

  it('the success screen links back to the tele appointments list', () => {
    fixture.componentInstance.selectSpecialty('CLINICA_GERAL');
    fixture.detectChanges();
    click('agendar-proximo');
    fixture.componentInstance.selectSlot('2026-07-10T09:00');
    fixture.detectChanges();
    click('agendar-proximo');
    click('agendar-confirmar');
    click('agendar-ver-agendamentos');
    expect(router.navigate).toHaveBeenCalledWith(['/telemedicina/agendamentos']);
  });
});
