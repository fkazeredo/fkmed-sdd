import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Domain-oriented API of the password-recovery flow (SPEC-0002 BR10; committed snapshot
 * docs/api/openapi.json). Neutral by design (BR7) — the request call answers 202 regardless of
 * whether the e-mail exists; the reset call answers 200 or 410 (invalid/used/expired link). */
@Injectable({ providedIn: 'root' })
export class RecoveryApi {
  private readonly http = inject(HttpClient);

  request(email: string): Observable<void> {
    return this.http.post<void>('/api/auth/recovery/request', { email });
  }

  reset(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>('/api/auth/recovery/reset', { token, newPassword });
  }
}
