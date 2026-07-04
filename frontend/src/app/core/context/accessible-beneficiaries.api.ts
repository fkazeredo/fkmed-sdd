import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * A beneficiary accessible to the authenticated user's family scope (SPEC-0003 BR1/BR5):
 * a titular sees themselves and their dependents; a dependent sees only themselves. The list
 * comes already filtered by the caller's scope — the server is the sole authority (BR3).
 */
export interface AccessibleBeneficiary {
  beneficiaryId: string;
  firstName: string;
  role: 'TITULAR' | 'DEPENDENT';
}

/** Contract of GET /api/context/accessible-beneficiaries (committed snapshot: docs/api/openapi.json). */
@Injectable({ providedIn: 'root' })
export class AccessibleBeneficiariesApi {
  private readonly http = inject(HttpClient);

  getAccessibleBeneficiaries(): Observable<AccessibleBeneficiary[]> {
    return this.http.get<AccessibleBeneficiary[]>('/api/context/accessible-beneficiaries');
  }
}
