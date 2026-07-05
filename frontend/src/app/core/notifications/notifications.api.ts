import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * A single notification (SPEC-0004 BR1): title, short text, timestamp, read/unread state
 * and, when applicable, a deep link to the resource inside the portal.
 */
export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  body: string;
  link: string | null;
  createdAt: string;
  read: boolean;
}

/**
 * GET /api/notifications response envelope: a newest-first page (BR3) plus the account's
 * total unread count (BR2) — `unread` always reflects the whole account, not just this page.
 */
export interface NotificationListResponse {
  unread: number;
  items: NotificationItem[];
}

/**
 * A notification-event-type catalog entry (SPEC-0004 BR5/BR7): registry data (code +
 * description) seeded by migration, the account's current e-mail opt-out and whether the type
 * is mandatory (security/account events — MUST NOT be disableable).
 */
export interface NotificationPreference {
  code: string;
  description: string;
  emailOptOut: boolean;
  mandatory: boolean;
}

const DEFAULT_PAGE_SIZE = 20;

/**
 * Contract of the notification endpoints (SPEC-0004, frozen by the architect's slice plan — no
 * committed OpenAPI snapshot yet since the backend module isn't built on this branch; the PUT
 * preferences body shape below — single `{code, emailOptOut}` — is this dev's minimal reading
 * of "PUT /api/notifications/preferences" and is flagged for the architect to confirm/re-sync
 * once the real backend contract lands).
 */
@Injectable({ providedIn: 'root' })
export class NotificationsApi {
  private readonly http = inject(HttpClient);

  list(page: number, size: number = DEFAULT_PAGE_SIZE): Observable<NotificationListResponse> {
    return this.http.get<NotificationListResponse>('/api/notifications', {
      params: { page, size },
    });
  }

  markRead(id: string): Observable<void> {
    return this.http.post<void>(`/api/notifications/${id}/read`, {});
  }

  markAllRead(): Observable<void> {
    return this.http.post<void>('/api/notifications/read-all', {});
  }

  getPreferences(): Observable<NotificationPreference[]> {
    return this.http.get<NotificationPreference[]>('/api/notifications/preferences');
  }

  updatePreference(code: string, emailOptOut: boolean): Observable<void> {
    return this.http.put<void>('/api/notifications/preferences', { code, emailOptOut });
  }
}
