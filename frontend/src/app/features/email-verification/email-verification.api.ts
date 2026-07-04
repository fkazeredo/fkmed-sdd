import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Domain-oriented API of the e-mail verification flow (SPEC-0002 BR5). */
@Injectable({ providedIn: 'root' })
export class EmailVerificationApi {
  private readonly http = inject(HttpClient);

  confirm(token: string): Observable<void> {
    return this.http.post<void>('/api/auth/verification/confirm', { token });
  }

  resend(email: string): Observable<void> {
    return this.http.post<void>('/api/auth/verification/resend', { email });
  }
}
