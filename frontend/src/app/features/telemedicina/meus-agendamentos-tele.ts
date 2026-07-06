import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { BeneficiaryContextService } from '../../core/context/beneficiary-context.service';
import { AppointmentView } from '../agendamento/appointments.api';
import { formatDayLabel, formatSlotTime } from '../agendamento/slot-picker';
import { isJoinWindowOpen } from './tele-time';
import { TeleApi } from './tele.api';

const JOIN_TICK_MS = 15_000;

/**
 * Telemedicine appointments (SPEC-0010 BR14) — the tele-filtered "Meus Agendamentos": upcoming and
 * history teleconsultations of all accessible beneficiaries (server-scoped, `telemedicine=true`),
 * each with the Telemedicina badge. "Entrar na consulta" is enabled only inside the join window
 * (10 min before the slot until its end — AC6); a live tick re-evaluates it without a reload. The
 * join calls `POST /api/appointments/{id}/join`; a `409 tele.join-window-closed` (a race against the
 * window) surfaces inline. On success it opens the live room.
 */
@Component({
  selector: 'app-meus-agendamentos-tele',
  imports: [FormsModule, TranslatePipe],
  templateUrl: './meus-agendamentos-tele.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeusAgendamentosTele implements OnInit, OnDestroy {
  private readonly api = inject(TeleApi);
  private readonly router = inject(Router);
  protected readonly context = inject(BeneficiaryContextService);

  protected readonly loading = signal(true);
  protected readonly errorKey = signal<string | null>(null);
  protected readonly upcoming = signal<AppointmentView[]>([]);
  protected readonly history = signal<AppointmentView[]>([]);
  protected readonly tab = signal<'proximos' | 'historico'>('proximos');
  protected readonly filterId = signal<string | null>(null);
  protected readonly now = signal(new Date());

  protected readonly joiningId = signal<string | null>(null);
  protected readonly joinErrorId = signal<string | null>(null);

  private ticker?: ReturnType<typeof setInterval>;

  protected readonly visible = computed(() =>
    this.tab() === 'proximos' ? this.upcoming() : this.history(),
  );

  ngOnInit(): void {
    this.load();
    this.ticker = setInterval(() => this.now.set(new Date()), JOIN_TICK_MS);
  }

  ngOnDestroy(): void {
    if (this.ticker) {
      clearInterval(this.ticker);
    }
  }

  load(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.api.getTeleAppointments(this.filterId() ?? undefined).subscribe({
      next: (list) => {
        this.upcoming.set(list.upcoming);
        this.history.set(list.history);
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

  subjectOf(appointment: AppointmentView): string {
    return appointment.specialtyName ?? appointment.specialtyCode ?? '';
  }

  whenOf(appointment: AppointmentView): string {
    const value = appointment.scheduledAt;
    return `${formatDayLabel(value.split('T')[0])} · ${formatSlotTime(value)}`;
  }

  /** AC6: the join button is enabled only within the window (recomputed each tick via `now`). */
  canJoin(appointment: AppointmentView): boolean {
    return isJoinWindowOpen(appointment.scheduledAt, this.now());
  }

  join(appointment: AppointmentView): void {
    if (this.joiningId()) {
      return;
    }
    this.joiningId.set(appointment.id);
    this.joinErrorId.set(null);
    this.api.joinScheduled(appointment.id).subscribe({
      next: () => {
        this.joiningId.set(null);
        void this.router.navigate(['/telemedicina/sessao']);
      },
      error: (error: HttpErrorResponse) => {
        this.joiningId.set(null);
        this.joinErrorId.set(appointment.id);
        // A window race is expected; anything else falls back to the generic message on reload.
        if (error.error?.code !== 'tele.join-window-closed') {
          this.errorKey.set('common.error');
        }
      },
    });
  }
}
