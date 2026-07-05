import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { NotificationPreference, NotificationsApi } from '../../core/notifications/notifications.api';

/**
 * E-mail notification preferences (SPEC-0004 BR7): per-event-type e-mail opt-out. Mandatory
 * types (security/account events — e.g. password changed, account locked) render locked: the
 * toggle is disabled so the user cannot even attempt the opt-out the backend would otherwise
 * refuse with `notification.preference-mandatory` (422). In-app delivery is always on and has
 * no toggle here (BR7).
 */
@Component({
  selector: 'app-notification-preferences',
  imports: [TranslatePipe],
  templateUrl: './notification-preferences.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationPreferences implements OnInit {
  private readonly api = inject(NotificationsApi);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly preferences = signal<NotificationPreference[]>([]);
  readonly savingCode = signal<string | null>(null);
  readonly errorKey = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.api.getPreferences().subscribe({
      next: (response) => {
        this.preferences.set(response);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  toggle(preference: NotificationPreference): void {
    if (preference.mandatory || this.savingCode()) {
      return;
    }
    const nextValue = !preference.emailOptOut;
    this.savingCode.set(preference.code);
    this.errorKey.set(null);
    this.api.updatePreference(preference.code, nextValue).subscribe({
      next: () => {
        this.preferences.update((list) =>
          list.map((current) =>
            current.code === preference.code ? { ...current, emailOptOut: nextValue } : current,
          ),
        );
        this.savingCode.set(null);
      },
      error: (error: HttpErrorResponse) => {
        this.savingCode.set(null);
        this.errorKey.set(
          error.error?.code === 'notification.preference-mandatory'
            ? 'notificacoes.preferencias.erro.mandatorio'
            : 'common.error',
        );
      },
    });
  }
}
