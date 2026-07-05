import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../auth/auth.service';
import { BeneficiaryContextService } from '../context/beneficiary-context.service';
import { NotificationsStateService } from '../notifications/notifications-state.service';
import { BeneficiarySelector } from './beneficiary-selector';
import { NotificationBell } from './notification-bell';

/**
 * App shell (SPEC-0001 §Scope): top bar (with the SPEC-0003 active-beneficiary selector and the
 * SPEC-0004 notification bell), main navigation placeholder and content outlet. Only mounted
 * behind `authGuard` (app.routes.ts), so loading the beneficiary context and the unread
 * notification count here always runs for an authenticated user.
 */
@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    TranslatePipe,
    ButtonModule,
    BeneficiarySelector,
    NotificationBell,
  ],
  templateUrl: './shell.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shell implements OnInit {
  protected readonly auth = inject(AuthService);
  private readonly context = inject(BeneficiaryContextService);
  private readonly notifications = inject(NotificationsStateService);

  ngOnInit(): void {
    this.context.load();
    this.notifications.refreshUnread();
  }

  logout(): void {
    this.auth.logout();
  }
}
