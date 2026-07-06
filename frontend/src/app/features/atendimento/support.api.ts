import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** The 4 channel types the backend recognizes (SPEC-0014 BR1). */
export type ChannelType = 'CENTRAL' | 'WHATSAPP' | 'OUVIDORIA' | 'ANS';

/** A support channel card (SPEC-0014 BR1/BR2) — FROZEN contract, exclusively from the registry. */
export interface SupportChannelView {
  type: ChannelType;
  label: string;
  value: string;
  hours: string | null;
  displayOrder: number;
}

/** The antifraud section content (SPEC-0014 BR3). */
export interface AntifraudView {
  title: string;
  message: string;
  bestPractices: string[];
}

/** The 6 fixed FAQ categories (SPEC-0014 BR5). */
export type FaqCategory =
  | 'REEMBOLSO'
  | 'CARTEIRINHA'
  | 'AGENDAMENTO'
  | 'TELEMEDICINA'
  | 'BOLETOS'
  | 'REDE';

/** One FAQ question matching the current filter (SPEC-0014 BR5). */
export interface FaqQuestionView {
  id: string;
  category: FaqCategory;
  question: string;
  answer: string;
  displayOrder: number;
}

/** `201` confirmation of a registered Libras service request (SPEC-0014 BR4). */
export interface LibrasRequestResponse {
  situation: 'REGISTERED';
  nextStep: 'videocall-shortly' | 'next-period';
  hours: string | null;
}

/**
 * Domain-oriented API of the Canais de Atendimento feature (SPEC-0014). Built against the FROZEN
 * contract from the slice plan — no raw HttpClient in components (frontend-angular.md §HTTP and
 * errors).
 */
@Injectable({ providedIn: 'root' })
export class SupportApi {
  private readonly http = inject(HttpClient);

  /** BR1/BR2: the channel cards, in content-defined order. */
  getChannels(): Observable<SupportChannelView[]> {
    return this.http.get<SupportChannelView[]>('/api/support/channels');
  }

  /** BR3: the fixed antifraud copy. */
  getAntifraud(): Observable<AntifraudView> {
    return this.http.get<AntifraudView>('/api/support/antifraud');
  }

  /** BR5/BR6: FAQ entries, optionally filtered by category and/or a real-time search term. Both
   * filters are sent to the server (case/accent-insensitive there) so the content-gap metric
   * (SPEC-0014 §Observability) reflects real searches. */
  getFaq(category?: FaqCategory | null, q?: string | null): Observable<FaqQuestionView[]> {
    let params = new HttpParams();
    if (category) {
      params = params.set('category', category);
    }
    if (q) {
      params = params.set('q', q);
    }
    return this.http.get<FaqQuestionView[]>('/api/support/faq', { params });
  }

  /** BR4: registers a Central de Libras service request for the given beneficiary. */
  requestLibras(beneficiaryId: string): Observable<LibrasRequestResponse> {
    return this.http.post<LibrasRequestResponse>('/api/support/libras-requests', { beneficiaryId });
  }
}
