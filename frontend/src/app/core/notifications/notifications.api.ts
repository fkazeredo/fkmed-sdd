import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

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
 * A notification-event-type catalog entry (SPEC-0004 BR5/BR7): registry data (`type` code +
 * description) seeded by migration, the account's current e-mail opt-out and whether the type
 * is mandatory (security/account events — MUST NOT be disableable).
 */
export interface NotificationPreference {
  type: string;
  description: string;
  emailOptOut: boolean;
  mandatory: boolean;
}

/**
 * GET/PUT /api/notifications/preferences response envelope (real backend contract, OpenAPI
 * snapshot): the catalog is wrapped in `{ preferences: [...] }`, never a bare array.
 */
export interface NotificationPreferencesResponse {
  preferences: NotificationPreference[];
}

const DEFAULT_PAGE_SIZE = 20;

/**
 * Contract of the notification endpoints (SPEC-0004), matching the real backend OpenAPI
 * snapshot. Preferences use an envelope (`{ preferences: [...] }`) and the event type key is
 * `type`; PUT is a batch that returns the updated catalog (200), so the service unwraps the
 * envelope and the caller reflects the returned catalog rather than assuming void.
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
    return this.http
      .get<NotificationPreferencesResponse>('/api/notifications/preferences')
      .pipe(map((response) => response.preferences));
  }

  /** Toggles a single event type's e-mail opt-out. The backend accepts a batch and returns the
   * full updated catalog (200) — a one-element batch serves the per-type toggle UX, and the
   * returned catalog is what the caller renders. */
  updatePreference(type: string, emailOptOut: boolean): Observable<NotificationPreference[]> {
    return this.http
      .put<NotificationPreferencesResponse>('/api/notifications/preferences', {
        preferences: [{ type, emailOptOut }],
      })
      .pipe(map((response) => response.preferences));
  }
}
