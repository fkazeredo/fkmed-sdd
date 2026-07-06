import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { formatElapsed } from './tele-time';
import { TeleApi, TeleSession } from './tele.api';
import { TeleInstabilityBanner } from './tele-instability-banner';
import { TeleSessionStreamService, TeleStreamError } from './tele-session-stream.service';

const DURATION_TICK_MS = 1000;

/**
 * Live telemedicine session screen — state-driven, no media (ADR-0015). One component renders the
 * whole lifecycle off the SSE stream (`TeleSessionStreamService`, BR6/ADR-0016):
 *  - EM_FILA (BR5/BR6/AC1): position + ETA updating live, plus "Sair da fila" (confirm → ABANDONADA,
 *    hub reopens — AC2), with the instability banner (BR1/AC7).
 *  - EM_ATENDIMENTO (BR8/BR9): highlighted "é a sua vez" + the room (professional name + CRM, start
 *    time, running duration ticking) and "Encerrar minha participação".
 *  - ENCERRADA (BR9/AC4): the closure summary — professional, duration, guidance and issued
 *    documents, with "Ver em Minha Saúde".
 *  - ABANDONADA (AC2/AC3): the left / no-show notice; the hub reopens.
 * A `404 tele.session-not-found` on connect means there is no active session → the empty state.
 */
@Component({
  selector: 'app-sessao-atendimento',
  imports: [RouterLink, TranslatePipe, TeleInstabilityBanner],
  templateUrl: './sessao-atendimento.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SessaoAtendimento implements OnInit, OnDestroy {
  private readonly stream = inject(TeleSessionStreamService);
  private readonly api = inject(TeleApi);
  private readonly router = inject(Router);

  protected readonly session = signal<TeleSession | null>(null);
  /** `not-found` = no active session (404); `transport` = the stream dropped. */
  protected readonly streamError = signal<'not-found' | 'transport' | null>(null);
  protected readonly now = signal(new Date());

  protected readonly confirmingLeave = signal(false);
  protected readonly leaving = signal(false);
  protected readonly leaveError = signal<string | null>(null);

  private streamSub?: Subscription;
  private ticker?: ReturnType<typeof setInterval>;

  protected readonly state = computed(() => this.session()?.state ?? null);
  protected readonly professional = computed(() => this.session()?.professional ?? null);
  protected readonly room = computed(() => this.session()?.room ?? null);

  /** BR9: running duration since the room start, recomputed every tick. */
  protected readonly runningDuration = computed(() => {
    const startedAt = this.room()?.startedAt;
    return startedAt ? formatElapsed(startedAt, this.now()) : null;
  });

  ngOnInit(): void {
    this.streamSub = this.stream.connect().subscribe({
      next: (session) => {
        this.session.set(session);
        this.streamError.set(null);
      },
      error: (error: unknown) => {
        this.streamError.set(error instanceof TeleStreamError && error.status === 404 ? 'not-found' : 'transport');
      },
    });
    this.ticker = setInterval(() => this.now.set(new Date()), DURATION_TICK_MS);
  }

  ngOnDestroy(): void {
    this.streamSub?.unsubscribe();
    if (this.ticker) {
      clearInterval(this.ticker);
    }
  }

  askLeave(): void {
    this.confirmingLeave.set(true);
    this.leaveError.set(null);
  }

  cancelLeave(): void {
    this.confirmingLeave.set(false);
  }

  /** AC2 (queue) / BR9 (room): leave the current session → ABANDONADA, then reopen the hub. */
  confirmLeave(): void {
    if (this.leaving()) {
      return;
    }
    this.leaving.set(true);
    this.leaveError.set(null);
    this.api.leaveSession().subscribe({
      next: () => {
        this.leaving.set(false);
        this.confirmingLeave.set(false);
        void this.router.navigate(['/telemedicina']);
      },
      error: (error: HttpErrorResponse) => {
        this.leaving.set(false);
        this.leaveError.set(error.error?.code === 'tele.session-not-found' ? 'tele.session-not-found' : 'common.error');
      },
    });
  }

  backToHub(): void {
    void this.router.navigate(['/telemedicina']);
  }
}
