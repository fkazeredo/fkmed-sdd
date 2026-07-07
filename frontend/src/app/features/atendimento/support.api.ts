import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/** A channel card (SPEC-0014 BR1). `sublabel` is present only for a type with more than one row
 * (Central 24h: "Capitais" / "Demais localidades"). FROZEN contract: docs/api/openapi.json. */
export interface SupportChannel {
  type: 'CENTRAL' | 'WHATSAPP' | 'OUVIDORIA' | 'ANS';
  label: string;
  sublabel?: string;
  value: string;
  hours?: string;
  order: number;
}

/** The antifraud section's operator content (BR3). */
export interface AntifraudContent {
  title: string;
  message: string;
}

/** The 6 fixed FAQ categories (BR5). */
export type FaqCategory = 'REEMBOLSO' | 'CARTEIRINHA' | 'AGENDAMENTO' | 'TELEMEDICINA' | 'BOLETOS' | 'REDE';

/** One FAQ question (BR5/BR6). */
export interface FaqEntry {
  id: string;
  category: FaqCategory;
  question: string;
  answer: string;
  order: number;
}

/** The outcome of registering a Libras service request (BR4). `hoursStart`/`hoursEnd` are present
 * only when `nextStep` is `'next-period'`. */
export interface LibrasRequestResult {
  situation: 'REGISTERED' | 'ATTENDED';
  nextStep: 'videocall-shortly' | 'next-period';
  hoursStart?: string;
  hoursEnd?: string;
}

/**
 * Domain-oriented API of the Atendimento feature (SPEC-0014), built against the FROZEN contract
 * (docs/api/openapi.json). No raw HttpClient in components (frontend-angular.md §HTTP and errors).
 */
@Injectable({ providedIn: 'root' })
export class SupportApi {
  private readonly http = inject(HttpClient);

  /** BR1: the channel cards, in display order. */
  getChannels(): Observable<SupportChannel[]> {
    return this.http.get<SupportChannel[]>('/api/support/channels');
  }

  /** BR3: the antifraud section content. */
  getAntifraud(): Observable<AntifraudContent> {
    return this.http.get<AntifraudContent>('/api/support/antifraud');
  }

  /** BR5: FAQ entries filtered by an optional category and/or a real-time search term. */
  getFaq(query: string, category: string | null): Observable<FaqEntry[]> {
    let params = new HttpParams();
    if (query) {
      params = params.set('q', query);
    }
    if (category) {
      params = params.set('category', category);
    }
    return this.http.get<FaqEntry[]>('/api/support/faq', { params });
  }

  /** BR4: registers a Libras service request for a beneficiary within the caller's scope. */
  requestLibras(beneficiaryId: string): Observable<LibrasRequestResult> {
    return this.http.post<LibrasRequestResult>('/api/support/libras-requests', { beneficiaryId });
  }
}
