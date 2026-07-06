import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { DialogModule } from 'primeng/dialog';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { Appointment, AppointmentsApi, AvailabilityDay } from './appointments.api';
import { formatDayLabel, formatSlotTime } from './slot-picker';
import { SlotPicker } from './slot-picker';

const ACTIVE_STATUSES: readonly Appointment['status'][] = ['AGENDADO', 'REAGENDADO'];

/**
 * Meus Agendamentos (SPEC-0009 BR13): commitments of ALL beneficiaries accessible to the user, with
 * a beneficiary filter (server-side via `?beneficiaryId=`). Tab Próximos (AGENDADO/REAGENDADO,
 * soonest-first) and tab Histórico (CANCELADO/REALIZADO, most-recent-first). Cancel (until the start
 * time, optional reason ≤ 200 — BR9) and Reschedule (only the date/time step reopens, same
 * protocol — BR10) act on active items only, each in a confirmation dialog; a slot-taken race on
 * reschedule keeps the dialog open with a fresh availability (BR6).
 */
@Component({
  selector: 'app-meus-agendamentos',
  imports: [FormsModule, TranslatePipe, DialogModule, SlotPicker],
  templateUrl: './meus-agendamentos.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeusAgendamentos implements OnInit {
  private readonly api = inject(AppointmentsApi);
  protected readonly context = inject(BeneficiaryContextService);

  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly appointments = signal<Appointment[]>([]);
  protected readonly tab = signal<'proximos' | 'historico'>('proximos');
  protected readonly filterId = signal<string | null>(null);

  protected readonly proximos = computed(() =>
    this.appointments()
      .filter((appointment) => ACTIVE_STATUSES.includes(appointment.status))
      .sort((a, b) => a.scheduledAt.localeCompare(b.scheduledAt)),
  );
  protected readonly historico = computed(() =>
    this.appointments()
      .filter((appointment) => !ACTIVE_STATUSES.includes(appointment.status))
      .sort((a, b) => b.scheduledAt.localeCompare(a.scheduledAt)),
  );
  protected readonly visible = computed(() =>
    this.tab() === 'proximos' ? this.proximos() : this.historico(),
  );

  // Cancel dialog (BR9).
  protected readonly cancelTarget = signal<Appointment | null>(null);
  protected readonly cancelReason = signal('');
  protected readonly cancelError = signal<string | null>(null);
  protected readonly cancelling = signal(false);

  // Reschedule dialog (BR10).
  protected readonly rescheduleTarget = signal<Appointment | null>(null);
  protected readonly rescheduleDays = signal<AvailabilityDay[]>([]);
  protected readonly rescheduleSlot = signal<string | null>(null);
  protected readonly rescheduleError = signal<string | null>(null);
  protected readonly rescheduling = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.getAppointments(this.filterId() ?? undefined).subscribe({
      next: (list) => {
        this.appointments.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.errorKey.set('common.error');
        this.loading.set(false);
      },
    });
  }

  setTab(tab: 'proximos' | 'historico'): void {
    this.tab.set(tab);
  }

  onFilterChange(value: string): void {
    this.filterId.set(value === '' ? null : value);
    this.load();
  }

  subjectOf(appointment: Appointment): string {
    return (appointment.type === 'EXAM' ? appointment.exam : appointment.specialty) ?? '';
  }

  whenOf(appointment: Appointment): string {
    const value = appointment.scheduledAt;
    return `${formatDayLabel(value.split('T')[0])} · ${formatSlotTime(value)}`;
  }

  /** BR9/BR10: cancel and reschedule act only on active commitments (the backend has already flipped
   * a passed appointment to REALIZADO — BR12 — so an active item is by definition before its start). */
  canModify(appointment: Appointment): boolean {
    return ACTIVE_STATUSES.includes(appointment.status);
  }

  // --- Cancel (BR9) ---
  askCancel(appointment: Appointment): void {
    this.cancelTarget.set(appointment);
    this.cancelReason.set('');
    this.cancelError.set(null);
  }

  closeCancel(): void {
    this.cancelTarget.set(null);
  }

  confirmCancel(): void {
    const target = this.cancelTarget();
    if (!target || this.cancelling()) {
      return;
    }
    const reason = this.cancelReason().trim();
    this.cancelling.set(true);
    this.cancelError.set(null);
    this.api.cancel(target.id, reason || undefined).subscribe({
      next: () => {
        this.cancelling.set(false);
        this.cancelTarget.set(null);
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.cancelling.set(false);
        this.cancelError.set(
          error.error?.code === 'appointment.cancel-too-late'
            ? 'appointment.cancel-too-late'
            : 'common.error',
        );
      },
    });
  }

  // --- Reschedule (BR10) ---
  askReschedule(appointment: Appointment): void {
    this.rescheduleTarget.set(appointment);
    this.rescheduleSlot.set(null);
    this.rescheduleError.set(null);
    this.rescheduleDays.set([]);
    this.loadRescheduleAvailability(appointment);
  }

  closeReschedule(): void {
    this.rescheduleTarget.set(null);
  }

  selectRescheduleSlot(slot: string): void {
    this.rescheduleSlot.set(slot);
    this.rescheduleError.set(null);
  }

  confirmReschedule(): void {
    const target = this.rescheduleTarget();
    const slot = this.rescheduleSlot();
    if (!target || !slot || this.rescheduling()) {
      return;
    }
    this.rescheduling.set(true);
    this.rescheduleError.set(null);
    this.api.reschedule(target.id, slot).subscribe({
      next: () => {
        this.rescheduling.set(false);
        this.rescheduleTarget.set(null);
        this.load();
      },
      error: (error: HttpErrorResponse) => {
        this.rescheduling.set(false);
        if (error.error?.code === 'appointment.slot-taken') {
          this.rescheduleSlot.set(null);
          this.rescheduleError.set('appointment.slot-taken');
          this.loadRescheduleAvailability(target);
          return;
        }
        this.rescheduleError.set(
          error.error?.code === 'appointment.time-conflict'
            ? 'appointment.time-conflict'
            : 'common.error',
        );
      },
    });
  }

  private loadRescheduleAvailability(appointment: Appointment): void {
    if (!appointment.unitId) {
      return;
    }
    this.api
      .getAvailability({
        unitId: appointment.unitId,
        specialty: appointment.specialtyCode ?? undefined,
        exam: appointment.examCode ?? undefined,
      })
      .subscribe((days) => this.rescheduleDays.set(days));
  }
}
