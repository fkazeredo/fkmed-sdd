import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { NotificationItem, NotificationsApi } from '../../core/notifications/notifications.api';
import { NotificationsStateService } from '../../core/notifications/notifications-state.service';

const PAGE_SIZE = 20;

/**
 * Notification center (SPEC-0004 BR1/BR2/BR3): newest-first paginated list, read/unread
 * styling, deep links to the resource, mark-one-read and mark-all-read. `unread` on every page
 * response is authoritative for the shell bell — kept in sync via NotificationsStateService so
 * marking an item read here reflects immediately in the header (BR2).
 */
@Component({
  selector: 'app-notification-center',
  imports: [TranslatePipe, RouterLink, DatePipe],
  templateUrl: './notification-center.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationCenter implements OnInit {
  private readonly api = inject(NotificationsApi);
  protected readonly state = inject(NotificationsStateService);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly items = signal<NotificationItem[]>([]);
  readonly hasMore = signal(false);
  readonly loadingMore = signal(false);

  private page = 0;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.page = 0;
    this.api.list(this.page, PAGE_SIZE).subscribe({
      next: (response) => {
        this.items.set(response.items);
        this.state.syncUnread(response.unread);
        this.hasMore.set(response.items.length === PAGE_SIZE);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  loadMore(): void {
    if (this.loadingMore()) {
      return;
    }
    this.loadingMore.set(true);
    const nextPage = this.page + 1;
    this.api.list(nextPage, PAGE_SIZE).subscribe({
      next: (response) => {
        this.page = nextPage;
        this.items.update((current) => [...current, ...response.items]);
        this.state.syncUnread(response.unread);
        this.hasMore.set(response.items.length === PAGE_SIZE);
        this.loadingMore.set(false);
      },
      // BR3: a load-more failure keeps the already-rendered items — only the action itself fails.
      error: () => this.loadingMore.set(false),
    });
  }

  markRead(item: NotificationItem): void {
    if (item.read) {
      return;
    }
    this.state.markRead(item.id).subscribe(() => {
      this.items.update((list) =>
        list.map((current) => (current.id === item.id ? { ...current, read: true } : current)),
      );
    });
  }

  markAllRead(): void {
    this.state.markAllRead().subscribe(() => {
      this.items.update((list) => list.map((current) => ({ ...current, read: true })));
    });
  }
}
