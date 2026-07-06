import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideI18n } from '../../core/i18n/provide-i18n';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { AppointmentsApi, AvailabilityDay, CareUnit, RegistryOption } from './appointments.api';
import { ConsultaWizard } from './consulta-wizard';

const SPECIALTIES: RegistryOption[] = [
  { code: 'CARDIOLOGIA', name: 'Cardiologia' },
  { code: 'DERMATOLOGIA', name: 'Dermatologia' },
];
const UNITS: CareUnit[] = [{ id: 'u1', name: 'Unidade Centro', address: 'Rua A, 10 — Centro' }];
const DAYS: AvailabilityDay[] = [
  { date: '2026-07-10', slots: [{ slot: '2026-07-10T09:00', remaining: 2, available: true }] },
];

describe('ConsultaWizard (BR3/BR2/BR6/BR7)', () => {
  let fixture: ComponentFixture<ConsultaWizard>;
  let api: {
    getSpecialties: ReturnType<typeof vi.fn>;
    getUnits: ReturnType<typeof vi.fn>;
    getAvailability: ReturnType<typeof vi.fn>;
    bookConsultation: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  const context = {
    active: () => ({ beneficiaryId: 'b1', firstName: 'PEDRO', role: 'DEPENDENT' as const }),
  };

  function el(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }
  function click(testid: string): void {
    (el().querySelector(`[data-testid="${testid}"]`) as HTMLElement).click();
    fixture.detectChanges();
  }

  async function setup(queryParams: Record<string, string> = {}): Promise<void> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ConsultaWizard],
      providers: [
        provideI18n(),
        { provide: AppointmentsApi, useValue: api },
        { provide: BeneficiaryContextService, useValue: context },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ConsultaWizard);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    api = {
      getSpecialties: vi.fn().mockReturnValue(of(SPECIALTIES)),
      getUnits: vi.fn().mockReturnValue(of(UNITS)),
      getAvailability: vi.fn().mockReturnValue(of(DAYS)),
      bookConsultation: vi.fn().mockReturnValue(of({ protocol: 'AG-20260704-0001', status: 'AGENDADO' })),
    };
    await setup();
  });

  it('SPEC-0011 BR6/AC4: pre-selects the specialty handed off by a referral\'s "Agendar consulta" (?especialidade=)', async () => {
    await setup({ especialidade: 'DERMATOLOGIA' });
    expect(fixture.componentInstance['specialtyCode']()).toBe('DERMATOLOGIA');
    expect(el().querySelector('[data-testid="consulta-especialidade-escolhida"]')?.textContent).toContain(
      'Dermatologia',
    );
  });

  it('SPEC-0011: an unknown ?especialidade= code is silently ignored (no pre-selection, no crash)', async () => {
    await setup({ especialidade: 'INEXISTENTE' });
    expect(fixture.componentInstance['specialtyCode']()).toBeNull();
  });

  it('loads the specialties on init', () => {
    expect(api.getSpecialties).toHaveBeenCalled();
    expect(el().querySelector('[data-testid="consulta-wizard-page"]')).not.toBeNull();
  });

  it('BR2: blocks advancing from the specialty step without a selection, showing a message', () => {
    click('consulta-proximo');
    expect(el().querySelector('[data-testid="consulta-gate"]')).not.toBeNull();
    // Still on the specialty step — units were never loaded.
    expect(api.getUnits).not.toHaveBeenCalled();
    expect(fixture.componentInstance['step']()).toBe('specialty');
  });

  it('advances to the unit step after choosing a specialty and loads units for it', () => {
    fixture.componentInstance.selectSpecialty('CARDIOLOGIA');
    fixture.detectChanges();
    click('consulta-proximo');
    expect(api.getUnits).toHaveBeenCalledWith({ specialty: 'CARDIOLOGIA' });
    expect(el().querySelector('[data-testid="unit-picker"]')).not.toBeNull();
  });

  it('BR2: blocks advancing from the unit step without a unit', () => {
    fixture.componentInstance.selectSpecialty('CARDIOLOGIA');
    fixture.detectChanges();
    click('consulta-proximo');
    click('consulta-proximo');
    expect(api.getAvailability).not.toHaveBeenCalled();
    expect(fixture.componentInstance['step']()).toBe('unit');
  });

  it('completes the wizard and confirms with the active beneficiary (BR1/BR7)', () => {
    fixture.componentInstance.selectSpecialty('CARDIOLOGIA');
    fixture.detectChanges();
    click('consulta-proximo'); // -> unit
    fixture.componentInstance.selectUnit('u1');
    fixture.detectChanges();
    click('consulta-proximo'); // -> slot
    expect(api.getAvailability).toHaveBeenCalledWith({ unitId: 'u1', specialty: 'CARDIOLOGIA' });
    fixture.componentInstance.selectSlot('2026-07-10T09:00');
    fixture.detectChanges();
    click('consulta-proximo'); // -> review

    expect(el().querySelector('[data-testid="revisao-beneficiario"]')?.textContent).toContain('PEDRO');
    expect(el().querySelector('[data-testid="revisao-especialidade"]')?.textContent).toContain('Cardiologia');
    expect(el().querySelector('[data-testid="revisao-unidade"]')?.textContent).toContain('Unidade Centro');
    expect(el().querySelector('[data-testid="revisao-data-hora"]')?.textContent).toContain('09:00');

    click('consulta-confirmar');
    expect(api.bookConsultation).toHaveBeenCalledWith({
      beneficiaryId: 'b1',
      specialty: 'CARDIOLOGIA',
      unitId: 'u1',
      slot: '2026-07-10T09:00',
    });
    expect(el().querySelector('[data-testid="consulta-protocolo"]')?.textContent).toContain('AG-20260704-0001');
  });

  it('BR6: on 409 slot-taken returns to the time step with the warning and reloads availability', () => {
    api.bookConsultation.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'appointment.slot-taken' }, status: 409 })),
    );
    fixture.componentInstance.selectSpecialty('CARDIOLOGIA');
    fixture.detectChanges();
    click('consulta-proximo');
    fixture.componentInstance.selectUnit('u1');
    fixture.detectChanges();
    click('consulta-proximo');
    fixture.componentInstance.selectSlot('2026-07-10T09:00');
    fixture.detectChanges();
    click('consulta-proximo'); // review
    api.getAvailability.mockClear();
    click('consulta-confirmar');

    expect(fixture.componentInstance['step']()).toBe('slot');
    expect(el().querySelector('[data-testid="consulta-slot-taken"]')).not.toBeNull();
    expect(api.getAvailability).toHaveBeenCalledTimes(1); // fresh reload
  });

  it('maps 409 time-conflict to an inline review message (BR8/AC5)', () => {
    api.bookConsultation.mockReturnValue(
      throwError(() => new HttpErrorResponse({ error: { code: 'appointment.time-conflict' }, status: 409 })),
    );
    fixture.componentInstance.selectSpecialty('CARDIOLOGIA');
    fixture.detectChanges();
    click('consulta-proximo');
    fixture.componentInstance.selectUnit('u1');
    fixture.detectChanges();
    click('consulta-proximo');
    fixture.componentInstance.selectSlot('2026-07-10T09:00');
    fixture.detectChanges();
    click('consulta-proximo');
    click('consulta-confirmar');

    expect(fixture.componentInstance['step']()).toBe('review');
    expect(el().querySelector('[data-testid="consulta-erro"]')).not.toBeNull();
  });

  it('resets the chosen unit and slot when the specialty changes', () => {
    fixture.componentInstance.selectSpecialty('CARDIOLOGIA');
    fixture.componentInstance.selectUnit('u1');
    fixture.componentInstance.selectSlot('2026-07-10T09:00');
    fixture.componentInstance.selectSpecialty('DERMATOLOGIA');
    expect(fixture.componentInstance['unitId']()).toBeNull();
    expect(fixture.componentInstance['slot']()).toBeNull();
  });
});
