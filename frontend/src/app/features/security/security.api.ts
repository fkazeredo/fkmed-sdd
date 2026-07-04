import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Domain-oriented API of the authenticated password change (SPEC-0002 BR11; committed snapshot
 * docs/api/openapi.json). */
@Injectable({ providedIn: 'root' })
export class SecurityApi {
  private readonly http = inject(HttpClient);

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>('/api/auth/password', { currentPassword, newPassword });
  }
}
