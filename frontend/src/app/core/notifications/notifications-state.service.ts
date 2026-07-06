import { inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { NotificationsApi } from './notifications.api';

/**
 * Shared unread-notification count (SPEC-0004 BR2): the shell bell and the notification center
 * both read this single signal, and every mutating action funnels through here — marking an
 * item read anywhere in the app updates the bell immediately, with no page reload or polling.
 */
@Injectable({ providedIn: 'root' })
export class NotificationsStateService {
  private readonly api = inject(NotificationsApi);

  readonly unread = signal(0);

  /** Loads just the unread count (BR2) via a minimal page fetch — the notification center owns
   * its own item listing separately. Called once on shell init, alongside the beneficiary
   * context (SPEC-0003 BR5). */
  refreshUnread(): void {
    this.api.list(0, 1).subscribe((response) => this.unread.set(response.unread));
  }

  /** Keeps the bell in sync with a page response the center screen already fetched — its
   * `unread` is authoritative (BR2), no separate round trip needed. */
  syncUnread(count: number): void {
    this.unread.set(count);
  }

  markRead(id: string): Observable<void> {
    return this.api.markRead(id).pipe(tap(() => this.unread.update((count) => Math.max(0, count - 1))));
  }

  markAllRead(): Observable<void> {
    return this.api.markAllRead().pipe(tap(() => this.unread.set(0)));
  }
}
