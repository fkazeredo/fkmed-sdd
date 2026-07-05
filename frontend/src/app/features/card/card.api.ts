import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * The Digital Card + data sheet of one beneficiary (SPEC-0007 BR1/BR9): visual-card fields
 * (`fullName`, `cardNumber`, `coverage`, `planName`, `planCategory`) plus the data-sheet-only
 * fields (`cns`, `ansRegistration`, `additives`). BR2: the visual card's coverage seal and the
 * data sheet's coverage field render from this same `coverage` value — single source of truth.
 * BR8: `cns` is shown in full ONLY on this screen and its PDF; everywhere else in the product it
 * is masked (no other screen renders it today).
 */
export interface CardResponse {
  fullName: string;
  cardNumber: string;
  cns: string;
  ansRegistration: string;
  coverage: string;
  planName: string;
  planCategory: string;
  additives: string[];
}

/**
 * Contract of GET /api/cards/{beneficiaryId} and GET /api/cards/{beneficiaryId}/pdf (frozen
 * contract — SPEC-0007 §API Contracts; both enforce SPEC-0003 scope, `404
 * context.beneficiary-not-accessible`; an inactive beneficiary answers `409 card.unavailable`).
 */
@Injectable({ providedIn: 'root' })
export class CardApi {
  private readonly http = inject(HttpClient);

  getCard(beneficiaryId: string): Observable<CardResponse> {
    return this.http.get<CardResponse>(`/api/cards/${beneficiaryId}`);
  }

  /** BR3: PDF for offline use — the caller triggers the browser download from the blob. */
  downloadPdf(beneficiaryId: string): Observable<Blob> {
    return this.http.get(`/api/cards/${beneficiaryId}/pdf`, { responseType: 'blob' });
  }
}
