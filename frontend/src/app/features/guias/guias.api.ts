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

/** Named period filter (BR2) — the backend's `GuidePeriod` enum accepts exactly these 3 values on
 * `?period=`; there is no custom `from`/`to` range (confirmed against the real controller +
 * OpenAPI snapshot at integration). */
export type GuidePeriodOption = 'LAST_30D' | 'LAST_90D' | 'LAST_12M';

/** List-item shape — the controller returns `List<GuideListItem>` with exactly these fields
 * (confirmed against the real contract at integration). */
export interface GuideCard {
  id: string;
  number: string;
  type: GuideType;
  requestingProvider: string;
  requestedAt: string;
  status: GuideStatus;
}

export interface GuideListFilters {
  beneficiaryId: string;
  status?: GuideStatus;
  period?: GuidePeriodOption;
}

/** One line of a guide's items table (BR5: TUSS code, description, quantity, item status). */
export interface GuideItemView {
  tussCode: string;
  description: string;
  quantity: number;
  status: GuideItemStatus;
}

/** Guide detail (BR5/BR7). `authExpired` drives BR7's "autorização expirada" notice; `denialReason`
 * carries BR5's NEGADA reason. */
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

/** `POST /api/tokens` / `GET /api/tokens/current` response (`TokenView {code, expiresAt}`). */
export interface TokenResponse {
  code: string;
  expiresAt: string;
}

/**
 * Domain-oriented API of the Guias e Token feature (SPEC-0012). Aligned to the real backend
 * contract at integration (OpenAPI snapshot + controllers): the list is a raw array, the period
 * filter is a 3-value enum, and the detail endpoint requires `beneficiaryId`. No raw HttpClient in
 * components (frontend-angular.md §HTTP and errors).
 */
@Injectable({ providedIn: 'root' })
export class GuidesApi {
  private readonly http = inject(HttpClient);

  /** BR1/BR2: the active beneficiary's guides, most-recent-first (backend's responsibility). The
   * controller returns a raw `List<GuideListItem>` — a bare JSON array, no `{items:[…]}` envelope. */
  getGuides(filters: GuideListFilters): Observable<GuideCard[]> {
    let params = new HttpParams().set('beneficiaryId', filters.beneficiaryId);
    if (filters.status) {
      params = params.set('status', filters.status);
    }
    if (filters.period) {
      params = params.set('period', filters.period);
    }
    return this.http.get<GuideCard[]>('/api/guides', { params });
  }

  /** BR5/BR7: full detail. `beneficiaryId` is required (the endpoint scopes the guide to the
   * beneficiary — an omitted param is a 400). A missing/out-of-scope id answers `404
   * guide.not-found`. */
  getGuide(id: string, beneficiaryId: string): Observable<GuideDetail> {
    const params = new HttpParams().set('beneficiaryId', beneficiaryId);
    return this.http.get<GuideDetail>(`/api/guides/${id}`, { params });
  }

  /** BR9/BR10: resumes an active token's countdown on screen load. A `404 token.none-active` means
   * no active token (not an error toast — see GuiasHub). `beneficiaryId` scopes the lookup to the
   * beneficiary the token belongs to (BR12). */
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
