import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Contract of GET /api/plan/my-plan (committed snapshot: docs/api/openapi.json). */
export interface MyPlanResponse {
  plan: PlanSummary;
  members: MemberSummary[];
}

export interface PlanSummary {
  name: string;
  ansRegistration: string;
  coverage: string;
  copay: boolean;
  reimbursement: boolean;
  additives: string[];
}

export interface MemberSummary {
  fullName: string;
  role: 'TITULAR' | 'DEPENDENT';
  cardNumber: string;
}

/** Domain-oriented API of the plan feature (no raw HttpClient in components). */
@Injectable({ providedIn: 'root' })
export class PlanApi {
  private readonly http = inject(HttpClient);

  getMyPlan(): Observable<MyPlanResponse> {
    return this.http.get<MyPlanResponse>('/api/plan/my-plan');
  }
}
