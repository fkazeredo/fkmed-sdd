import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { NotificationsStateService } from '../notifications/notifications-state.service';

/**
 * Bell entry point in the shell header (SPEC-0004 BR2): shows the unread count and links to
 * the notification center. The count is loaded once by the shell on init (mirrors the
 * active-beneficiary context, SPEC-0003 BR5) and kept live afterwards by
 * NotificationsStateService as items are marked read anywhere in the app — this component only
 * renders the shared signal, it never fetches on its own.
 */
@Component({
  selector: 'app-notification-bell',
  imports: [RouterLink],
  templateUrl: './notification-bell.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationBell {
  protected readonly state = inject(NotificationsStateService);
  private readonly translate = inject(TranslateService);

  /** Cap the visible badge at "99+" — an unbounded count would break the layout. */
  protected readonly badgeLabel = computed(() => (this.state.unread() > 99 ? '99+' : `${this.state.unread()}`));

  protected readonly ariaLabel = computed(() =>
    this.translate.instant('notificacoes.sino.aria', { count: this.state.unread() }),
  );
}
