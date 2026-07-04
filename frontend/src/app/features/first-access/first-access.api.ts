import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Identity-triple verification request (SPEC-0002 BR1; committed snapshot docs/api/openapi.json). */
export interface VerifyFirstAccessRequest {
  cpf: string;
  cardNumber: string;
  birthDate: string;
}

export interface VerifyFirstAccessResponse {
  registrationToken: string;
}

/** Account-creation request (SPEC-0002 BR4/BR9/BR15). Acceptance is server-authoritative (DL-0001). */
export interface CompleteFirstAccessRequest {
  registrationToken: string;
  email: string;
  password: string;
  acceptedTerms: boolean;
  acceptedPrivacy: boolean;
}

/** Domain-oriented API of the first-access flow (no raw HttpClient in components). */
@Injectable({ providedIn: 'root' })
export class FirstAccessApi {
  private readonly http = inject(HttpClient);

  verify(request: VerifyFirstAccessRequest): Observable<VerifyFirstAccessResponse> {
    return this.http.post<VerifyFirstAccessResponse>('/api/auth/first-access/verify', request);
  }

  complete(request: CompleteFirstAccessRequest): Observable<void> {
    return this.http.post<void>('/api/auth/first-access/complete', request);
  }
}
