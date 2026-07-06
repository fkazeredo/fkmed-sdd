import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { SearchableOptionList, SelectableOption } from '../../shared/components/searchable-option-list';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { AvailabilityDay, RegistryOption } from '../agendamento/appointments.api';
import { formatDayLabel, formatSlotTime, SlotPicker } from '../agendamento/slot-picker';
import { TeleApi } from './tele.api';

/** BR14 flow: specialty → date/time (tele agenda) → review → confirm. `success` is the protocol screen. */
type Step = 'specialty' | 'slot' | 'review' | 'success';
const FLOW: Step[] = ['specialty', 'slot', 'review', 'success'];

/**
 * Scheduled teleconsultation booking (SPEC-0010 BR14) — a specialty → tele-agenda slot → review →
 * confirm wizard on the SPEC-0009 appointment machinery (DL-0018: telemedicine modality, virtual
 * unit resolved server-side, so there is no unit step). Mirrors the consultation wizard's gate/step
 * pattern; the appointment is bound to the ACTIVE beneficiary at confirmation and carries the
 * Telemedicina badge. A slot-taken race returns to the time step with a fresh agenda (BR6, inherited).
 */
@Component({
  selector: 'app-agendar-teleconsulta',
  imports: [TranslatePipe, SearchableOptionList, SlotPicker],
  templateUrl: './agendar-teleconsulta.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgendarTeleconsulta implements OnInit {
  private readonly api = inject(TeleApi);
  private readonly router = inject(Router);
  private readonly context = inject(BeneficiaryContextService);

  protected readonly step = signal<Step>('specialty');
  protected readonly loading = signal(false);
  protected readonly gateKey = signal<string | null>(null);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly protocol = signal<string | null>(null);

  protected readonly specialties = signal<RegistryOption[]>([]);
  protected readonly days = signal<AvailabilityDay[]>([]);

  protected readonly specialtyCode = signal<string | null>(null);
  protected readonly specialtyName = signal<string | null>(null);
  protected readonly slot = signal<string | null>(null);

  protected readonly specialtyOptions = computed<SelectableOption[]>(() =>
    this.specialties().map((option) => ({ value: option.code, label: option.name })),
  );
  protected readonly beneficiaryName = computed(() => this.context.active()?.firstName ?? '');
  protected readonly slotLabel = computed(() => {
    const value = this.slot();
    return value ? `${formatDayLabel(value.split('T')[0])} · ${formatSlotTime(value)}` : '';
  });

  ngOnInit(): void {
    this.api.getTeleSpecialties().subscribe((list) => this.specialties.set(list));
  }

  selectSpecialty(code: string): void {
    const found = this.specialties().find((option) => option.code === code);
    if (!found) {
      return;
    }
    this.specialtyCode.set(found.code);
    this.specialtyName.set(found.name);
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
        this.gateKey.set('telemedicina.gate.especialidade');
        return;
      }
      this.loadAvailability();
      this.go('slot');
    } else if (current === 'slot') {
      if (!this.slot()) {
        this.gateKey.set('telemedicina.gate.horario');
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
    const slot = this.slot();
    if (!beneficiaryId || !specialty || !slot || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.bookTeleConsultation({ beneficiaryId, specialty, slot }).subscribe({
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
    void this.router.navigate(['/telemedicina/agendamentos']);
  }

  private go(step: Step): void {
    this.gateKey.set(null);
    this.step.set(step);
  }

  private loadAvailability(): void {
    const specialty = this.specialtyCode();
    if (!specialty) {
      return;
    }
    this.api.getTeleAvailability(specialty).subscribe((days) => this.days.set(days));
  }

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
      code === 'appointment.time-conflict' || code === 'appointment.outside-horizon' ? code : 'common.error',
    );
  }
}
