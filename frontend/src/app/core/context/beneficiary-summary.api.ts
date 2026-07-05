import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Full identity summary of one beneficiary within the caller's family scope (SPEC-0003,
 * already in `develop` since slice 1.3) — the source of the SPEC-0005 Home beneficiary card.
 * `avatarUrl` is always `null` in this phase (no profile photo until SPEC-0006).
 */
export interface BeneficiarySummary {
  firstName: string;
  fullName: string;
  role: 'TITULAR' | 'DEPENDENT';
  planName: string;
  cardNumber: string;
  avatarUrl: string | null;
}

/** Contract of GET /api/context/beneficiaries/{beneficiaryId} (committed snapshot: docs/api/openapi.json). */
@Injectable({ providedIn: 'root' })
export class BeneficiarySummaryApi {
  private readonly http = inject(HttpClient);

  getBeneficiary(beneficiaryId: string): Observable<BeneficiarySummary> {
    return this.http.get<BeneficiarySummary>(`/api/context/beneficiaries/${beneficiaryId}`);
  }
}
