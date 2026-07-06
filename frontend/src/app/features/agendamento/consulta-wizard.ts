import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SelectableOption, SearchableOptionList } from '../../shared/components/searchable-option-list';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { AppointmentsApi, AvailabilityDay, CareUnit, RegistryOption } from './appointments.api';
import { formatDayLabel, formatSlotTime } from './slot-picker';
import { SlotPicker } from './slot-picker';
import { UnitPicker } from './unit-picker';

/** The five wizard steps (BR3). `success` is the post-confirmation protocol screen (BR7). */
type Step = 'specialty' | 'unit' | 'slot' | 'review' | 'success';
const FLOW: Step[] = ['specialty', 'unit', 'slot', 'review', 'success'];

/**
 * Consultation booking wizard (SPEC-0009 BR3): specialty → unit → date/time → review → confirm, in
 * a single component with an internal step index (the first-access-wizard pattern). Step-gating
 * (BR2) blocks "Próximo" without the step's selection and shows a clear message. On confirm a
 * `409 appointment.slot-taken` returns the user to the time step with the "horário acabou de ser
 * preenchido" warning and a fresh availability load (BR6). The appointment binds to the ACTIVE
 * beneficiary read at confirmation time (BR1). SPEC-0011 BR6/AC4: reached from a referral's
 * "Agendar consulta" with an optional `?especialidade=<code>` query param — once the specialty
 * list loads, a matching code is pre-selected (the user still sees/can change it on this step).
 */
@Component({
  selector: 'app-consulta-wizard',
  imports: [TranslatePipe, SearchableOptionList, UnitPicker, SlotPicker],
  templateUrl: './consulta-wizard.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConsultaWizard implements OnInit {
  private readonly api = inject(AppointmentsApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly context = inject(BeneficiaryContextService);

  protected readonly step = signal<Step>('specialty');
  protected readonly loading = signal(false);
  /** BR2 inline gate message when the user tries to advance without the step's selection. */
  protected readonly gateKey = signal<string | null>(null);
  /** BR6/error mapping shown on the confirm/time step. */
  protected readonly errorKey = signal<string | null>(null);
  protected readonly protocol = signal<string | null>(null);

  protected readonly specialties = signal<RegistryOption[]>([]);
  protected readonly units = signal<CareUnit[]>([]);
  protected readonly days = signal<AvailabilityDay[]>([]);

  protected readonly specialtyCode = signal<string | null>(null);
  protected readonly specialtyName = signal<string | null>(null);
  protected readonly unitId = signal<string | null>(null);
  protected readonly slot = signal<string | null>(null);

  protected readonly specialtyOptions = computed<SelectableOption[]>(() =>
    this.specialties().map((option) => ({ value: option.code, label: option.name })),
  );
  protected readonly unitName = computed(
    () => this.units().find((unit) => unit.id === this.unitId())?.name ?? '',
  );
  protected readonly beneficiaryName = computed(() => this.context.active()?.firstName ?? '');
  protected readonly slotLabel = computed(() => {
    const value = this.slot();
    return value ? `${formatDayLabel(value.split('T')[0])} · ${formatSlotTime(value)}` : '';
  });

  ngOnInit(): void {
    this.api.getSpecialties().subscribe((list) => {
      this.specialties.set(list);
      // SPEC-0011 BR6/AC4: pre-select the specialty handed off by a referral's "Agendar consulta",
      // when its code matches one of the loaded specialties (silently ignored otherwise).
      const preselected = this.route.snapshot.queryParamMap.get('especialidade');
      if (preselected && list.some((option) => option.code === preselected)) {
        this.selectSpecialty(preselected);
      }
    });
  }

  selectSpecialty(code: string): void {
    const found = this.specialties().find((option) => option.code === code);
    if (!found) {
      return;
    }
    this.specialtyCode.set(found.code);
    this.specialtyName.set(found.name);
    // Unit and slot depend on the specialty — reset them so a changed specialty cannot keep a stale
    // unit/slot on review.
    this.unitId.set(null);
    this.slot.set(null);
    this.gateKey.set(null);
  }

  selectUnit(id: string): void {
    this.unitId.set(id);
    this.slot.set(null);
    this.gateKey.set(null);
  }

  selectSlot(value: string): void {
    this.slot.set(value);
    this.gateKey.set(null);
    this.errorKey.set(null);
  }

  next(): void {
    const current = this.step();
    if (current === 'specialty') {
      if (!this.specialtyCode()) {
        this.gateKey.set('agendamento.gate.especialidade');
        return;
      }
      this.loadUnits();
      this.go('unit');
    } else if (current === 'unit') {
      if (!this.unitId()) {
        this.gateKey.set('agendamento.gate.unidade');
        return;
      }
      this.loadAvailability();
      this.go('slot');
    } else if (current === 'slot') {
      if (!this.slot()) {
        this.gateKey.set('agendamento.gate.horario');
        return;
      }
      this.go('review');
    }
  }

  back(): void {
    const index = FLOW.indexOf(this.step());
    if (index > 0) {
      this.go(FLOW[index - 1]);
    }
  }

  confirm(): void {
    const beneficiaryId = this.context.active()?.beneficiaryId;
    const specialty = this.specialtyCode();
    const unitId = this.unitId();
    const slot = this.slot();
    if (!beneficiaryId || !specialty || !unitId || !slot || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.bookConsultation({ beneficiaryId, specialty, unitId, slot }).subscribe({
      next: (confirmation) => {
        this.loading.set(false);
        this.protocol.set(confirmation.protocol);
        this.go('success');
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.applyBookingError(error);
      },
    });
  }

  goToMeus(): void {
    void this.router.navigate(['/agendamento/meus']);
  }

  private go(step: Step): void {
    this.gateKey.set(null);
    this.step.set(step);
  }

  private loadUnits(): void {
    this.api.getUnits({ specialty: this.specialtyCode() ?? undefined }).subscribe((list) => this.units.set(list));
  }

  private loadAvailability(): void {
    const unitId = this.unitId();
    if (!unitId) {
      return;
    }
    this.api
      .getAvailability({ unitId, specialty: this.specialtyCode() ?? undefined })
      .subscribe((days) => this.days.set(days));
  }

  /** BR6: the slot-taken loser is returned to the time step with the warning and fresh availability;
   * the other business errors surface inline on the review step. */
  private applyBookingError(error: HttpErrorResponse): void {
    const code = error.error?.code;
    if (code === 'appointment.slot-taken') {
      this.slot.set(null);
      this.loadAvailability();
      this.go('slot');
      this.errorKey.set('appointment.slot-taken');
      return;
    }
    this.errorKey.set(
      code === 'appointment.time-conflict' || code === 'appointment.outside-horizon'
        ? code
        : 'common.error',
    );
  }
}
