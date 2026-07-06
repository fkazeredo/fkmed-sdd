import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** Guide state machine (SPEC-0012 BR6 — state machine, kept as an enum per the Javadoc-equivalent
 * keep criterion). `EM_ANALISE` is the only non-final start state; `EXECUTADA`/`NEGADA`/`CANCELADA`
 * are finals. */
export type GuideStatus =
  | 'EM_ANALISE'
  | 'AUTORIZADA'
  | 'PARCIALMENTE_AUTORIZADA'
  | 'NEGADA'
  | 'CANCELADA'
  | 'EXECUTADA';

/** Guide-item state machine (BR6). */
export type GuideItemStatus = 'EM_ANALISE' | 'AUTORIZADO' | 'NEGADO';

/** Fixed guide-type list (BR2: "Consulta · SP/SADT–Exames · Internação") — display labels come
 * from the bundle (`guias.tipo.*`), never from the wire code directly. */
export type GuideType = 'CONSULTA' | 'SP_SADT' | 'INTERNACAO';

/** Named period filters (BR2), mirroring `PeriodOption` in minha-saude/clinical-documents.api.ts —
 * `CUSTOM` is a client-only marker that sends `from`/`to` instead of `period`. */
export type GuidePeriodOption = 'P30D' | 'P90D' | 'P365D' | 'CUSTOM';

/** List-card shape (SPEC-0012 §Input/Output Examples + §Persistence Changes field list) — this
 * dev's camelCase reading of the snake_case `guide` table columns (id, number, type,
 * requesting_provider, requested_at, status); no literal list-card JSON example was given in the
 * spec (only the detail response was shown verbatim). Flag for integration reconciliation. */
export interface GuideCard {
  id: string;
  number: string;
  type: GuideType;
  requestingProvider: string;
  requestedAt: string;
  status: GuideStatus;
}

/** Bare `{items:[...]}` envelope — mirrors clinical-documents.api.ts's `ClinicalDocumentListResponse`
 * (Phase-4 lesson: consume exactly this shape, no extra wrapping). */
export interface GuideListResponse {
  items: GuideCard[];
}

export interface GuideListFilters {
  beneficiaryId: string;
  status?: GuideStatus;
  period?: Exclude<GuidePeriodOption, 'CUSTOM'>;
  from?: string;
  to?: string;
}

/** One line of a guide's items table (BR5: TUSS code, description, quantity, item status). */
export interface GuideItemView {
  tussCode: string;
  description: string;
  quantity: number;
  status: GuideItemStatus;
}

/** Guide detail (BR5/BR7) — the spec's literal example is
 * `{"status":"AUTORIZADA","authPassword":"AUT-482913","authValidUntil":"2026-08-03","items":[…]}`;
 * `authExpired` (BR7's "autorização expirada" notice) and `denialReason` (BR5, NEGADA) are this
 * dev's reading of the remaining business rules — flagged for integration reconciliation. */
export interface GuideDetail {
  id: string;
  number: string;
  type: GuideType;
  requestingProvider: string;
  requestedAt: string;
  status: GuideStatus;
  items: GuideItemView[];
  authPassword?: string;
  authValidUntil?: string;
  authExpired?: boolean;
  denialReason?: string;
}

/** `POST /api/tokens` / `GET /api/tokens/current` response (SPEC-0012 §Input/Output Examples:
 * `{"code":"483920","expiresAt":"…+10min"}`). */
export interface TokenResponse {
  code: string;
  expiresAt: string;
}

/**
 * Domain-oriented API of the Guias e Token feature (SPEC-0012). Built against the FROZEN contract
 * (spec §API Contracts + §Input/Output Examples — the slice plan's frozen-contract doc was not
 * found in the repo at handoff time; the spec's own literal examples and field list are the
 * source of truth here, see this dev's report). No raw HttpClient in components
 * (frontend-angular.md §HTTP and errors).
 */
@Injectable({ providedIn: 'root' })
export class GuidesApi {
  private readonly http = inject(HttpClient);

  /** BR1/BR2: the active beneficiary's guides, most-recent-first (backend's responsibility for a
   * single request — no client-side merge/sort here, unlike the multi-category Minha Saúde case). */
  getGuides(filters: GuideListFilters): Observable<GuideListResponse> {
    let params = new HttpParams().set('beneficiaryId', filters.beneficiaryId);
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.period) {
      params = params.set('period', filters.period);
    }
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    return this.http.get<GuideListResponse>('/api/guides', { params });
  }

  /** BR5/BR7: full detail. A missing/out-of-scope id answers `404 guide.not-found`. */
  getGuide(id: string): Observable<GuideDetail> {
    return this.http.get<GuideDetail>(`/api/guides/${id}`);
  }

  /** BR9/BR10: resumes an active token's countdown on screen load. A `404 token.none-active` means
   * no active token (not an error toast — see GuiasHub). `beneficiaryId` scopes the lookup to the
   * beneficiary the token belongs to (BR12) — not shown in the spec's literal example (which omits
   * query params), this dev's reading of the app's established per-beneficiary-resource convention
   * (e.g. clinical-documents/cards), flagged for integration reconciliation. */
  getCurrentToken(beneficiaryId: string): Observable<TokenResponse> {
    const params = new HttpParams().set('beneficiaryId', beneficiaryId);
    return this.http.get<TokenResponse>('/api/tokens/current', { params });
  }

  /** BR9: generates a new token for the given beneficiary — invalidates any previous one
   * immediately (server-side; the client just displays the new code/expiry). */
  generateToken(beneficiaryId: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/tokens', { beneficiaryId });
  }
}
