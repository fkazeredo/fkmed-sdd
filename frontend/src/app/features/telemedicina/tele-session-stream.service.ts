import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { TeleSession } from './tele.api';

/** Raised when the SSE endpoint answers non-2xx (notably `404 tele.session-not-found`) so the
 * caller can distinguish "no active session" from a transport drop. */
export class TeleStreamError extends Error {
  constructor(readonly status: number) {
    super(`tele stream failed with status ${status}`);
    this.name = 'TeleStreamError';
  }
}

/** Pure SSE frame splitter: pulls the `data:` payload of every COMPLETE event (`\n\n`-terminated)
 * out of the accumulated buffer, returning the leftover partial frame. Keep-alive comment lines
 * (`: ...`) and non-`data:` fields are ignored. Exported for direct unit testing. */
export function extractSseEvents(buffer: string): { events: string[]; rest: string } {
  const normalized = buffer.replace(/\r\n/g, '\n');
  const frames = normalized.split('\n\n');
  const rest = frames.pop() ?? '';
  const events: string[] = [];
  for (const frame of frames) {
    const data = frame
      .split('\n')
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).replace(/^ /, ''))
      .join('\n');
    if (data) {
      events.push(data);
    }
  }
  return { events, rest };
}

/**
 * Live telemedicine session stream (SPEC-0010 BR6, ADR-0016/DL-0022). Opens the frozen
 * `GET /api/tele/sessions/current` with `Accept: text/event-stream` and emits one `TeleSession` per
 * server event (position/ETA/state pushed on a short cadence + on the beneficiary's own
 * transitions). `fetch` streaming is used rather than `EventSource` because the request must carry
 * the OIDC Bearer header, which `EventSource` cannot set. The same endpoint is also a plain JSON GET
 * (`TeleApi.getCurrentSession`) for tests/fallback — the payload shape is identical.
 */
@Injectable({ providedIn: 'root' })
export class TeleSessionStreamService {
  private readonly auth = inject(AuthService);

  connect(): Observable<TeleSession> {
    return new Observable<TeleSession>((subscriber) => {
      const controller = new AbortController();
      const decoder = new TextDecoder();
      let buffer = '';

      const headers: Record<string, string> = { Accept: 'text/event-stream' };
      const token = this.auth.accessToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      fetch('/api/tele/sessions/current', { headers, signal: controller.signal })
        .then(async (response) => {
          if (!response.ok || !response.body) {
            subscriber.error(new TeleStreamError(response.status));
            return;
          }
          const reader = response.body.getReader();
          for (;;) {
            const { done, value } = await reader.read();
            if (done) {
              break;
            }
            buffer += decoder.decode(value, { stream: true });
            const { events, rest } = extractSseEvents(buffer);
            buffer = rest;
            for (const data of events) {
              try {
                subscriber.next(JSON.parse(data) as TeleSession);
              } catch {
                // A non-JSON frame is a keep-alive/comment — ignore it, keep streaming.
              }
            }
          }
          subscriber.complete();
        })
        .catch((error: unknown) => {
          // An abort from unsubscription is expected teardown, not an error to surface.
          if (!controller.signal.aborted) {
            subscriber.error(error);
          }
        });

      return () => controller.abort();
    });
  }
}
