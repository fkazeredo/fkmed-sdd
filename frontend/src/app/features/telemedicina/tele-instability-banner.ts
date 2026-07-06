import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { InstabilityNotice, TeleApi } from './tele.api';

/**
 * Instability banner (SPEC-0010 BR1/AC7): shows the active "Instabilidade momentânea da
 * Telemedicina" notice (SPEC-0005 content) on the hub AND the queue. Informative, NEVER blocking —
 * it only renders when a notice is active and is purely presentational; the user can still enter the
 * queue with it visible. Self-loads so any host drops it in with `<app-tele-instability-banner />`.
 * A content-endpoint failure resolves to "no banner" and is swallowed (it must never break the page).
 */
@Component({
  selector: 'app-tele-instability-banner',
  imports: [TranslatePipe],
  templateUrl: './tele-instability-banner.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TeleInstabilityBanner implements OnInit {
  private readonly api = inject(TeleApi);

  protected readonly notice = signal<InstabilityNotice | null>(null);

  ngOnInit(): void {
    this.api.getActiveInstabilityNotice().subscribe({
      next: (notice) => this.notice.set(notice),
      error: () => this.notice.set(null),
    });
  }
}
