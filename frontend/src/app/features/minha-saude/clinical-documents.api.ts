import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** The 4 clinical-document categories the backend recognizes (SPEC-0011 §API Contracts). The
 * "Receituários/Atestados" hub category (BR1) maps to two of these (PRESCRIPTION + SICK_NOTE) —
 * there is no combined category code on the wire. */
export type DocumentCategory = 'EXAM_ORDER' | 'REFERRAL' | 'PRESCRIPTION' | 'SICK_NOTE';

/** Named period filters (BR2); `CUSTOM` is a client-only marker — it sends `from`/`to` instead of
 * `period` (frozen contract: "+ custom from/to"). */
export type PeriodOption = 'P30D' | 'P90D' | 'P365D' | 'CUSTOM';

/** List-card shape — FROZEN (SPEC-0011 §Input/Output Examples): every field the wire sends, no
 * more. There is no "title" field; the card's displayed title is derived client-side from `type`
 * (see `document-format.ts`). `validUntil` is `null` for sick notes (BR4: no validity). */
export interface ClinicalDocumentCard {
  id: string;
  type: DocumentCategory;
  professional: string;
  crm: string;
  issuedAt: string;
  beneficiary: string;
  validUntil: string | null;
  expired: boolean;
}

/** Bare `{items:[...]}` envelope — no other wrapping (Phase-3 lesson: consume exactly this shape). */
export interface ClinicalDocumentListResponse {
  items: ClinicalDocumentCard[];
}

export interface DocumentListFilters {
  category: DocumentCategory;
  /** `'all'` or a beneficiary id — always sent (the frozen contract has no "omitted" case). */
  beneficiaryId: string;
  period?: Exclude<PeriodOption, 'CUSTOM'>;
  from?: string;
  to?: string;
}

/** One requested exam of an EXAM_ORDER detail (persistence §exam_order_item). */
export interface ExamOrderItem {
  name: string;
  tuss: string;
}

/** One medication of a PRESCRIPTION detail (persistence §prescription_item). */
export interface PrescriptionItem {
  medication: string;
  posology: string;
  guidance: string;
}

/**
 * Type-specific detail (SPEC-0011 BR6) — NOT part of the frozen contract's literal JSON example
 * (only the list envelope and the PDF endpoint were given verbatim); this shape is this dev's
 * reasonable reading of BR6 + the §Persistence Changes field list, kept flat (one optional field
 * group per type) to mirror `AppointmentView` (features/agendamento/appointments.api.ts) rather
 * than a discriminated union — flag for integration reconciliation with `--docs-be` (see report).
 */
export interface ClinicalDocumentDetail {
  id: string;
  type: DocumentCategory;
  professional: string;
  crm: string;
  issuedAt: string;
  beneficiary: string;
  validUntil: string | null;
  expired: boolean;
  // EXAM_ORDER
  exams?: ExamOrderItem[];
  clinicalIndication?: string;
  // REFERRAL — `specialtyCode` is the value handed to the SPEC-0009 wizard for pre-selection (AC4).
  specialtyCode?: string;
  specialtyName?: string;
  reason?: string;
  // PRESCRIPTION
  medications?: PrescriptionItem[];
  // SICK_NOTE — DL-0020: the CID IS shown.
  periodStart?: string;
  periodEnd?: string;
  cid?: string;
  notes?: string;
}

/**
 * Domain-oriented API of the Minha Saúde / Clinical Documents feature (SPEC-0011). Built against
 * the FROZEN contract from the slice plan — no raw HttpClient in components
 * (frontend-angular.md §HTTP and errors).
 */
@Injectable({ providedIn: 'root' })
export class ClinicalDocumentsApi {
  private readonly http = inject(HttpClient);

  /** BR2: combined filters — category, beneficiary (always sent, `'all'` default) and period
   * (named or custom `from`/`to`); ordering is the backend's responsibility for a single category. */
  getDocuments(filters: DocumentListFilters): Observable<ClinicalDocumentListResponse> {
    let params = new HttpParams()
      .set('category', filters.category)
      .set('beneficiaryId', filters.beneficiaryId);
    if (filters.period) {
      params = params.set('period', filters.period);
    }
    if (filters.from) {
      params = params.set('from', filters.from);
    }
    if (filters.to) {
      params = params.set('to', filters.to);
    }
    return this.http.get<ClinicalDocumentListResponse>('/api/clinical-documents', { params });
  }

  /** BR6: type-specific detail. A missing/out-of-scope id answers `404 document.not-found`
   * (existence not revealed). */
  getDocument(id: string): Observable<ClinicalDocumentDetail> {
    return this.http.get<ClinicalDocumentDetail>(`/api/clinical-documents/${id}`);
  }

  /** BR7/BR5: PDF for offline use — the caller triggers the browser download from the blob; a
   * document stays downloadable after it expires. */
  downloadPdf(id: string): Observable<Blob> {
    return this.http.get(`/api/clinical-documents/${id}/pdf`, { responseType: 'blob' });
  }
}
