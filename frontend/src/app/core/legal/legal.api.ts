import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type LegalDocumentType = 'TERMS' | 'PRIVACY';

/**
 * A versioned legal document and my acceptance state (SPEC-0006 BR8), as returned by
 * `GET /api/legal-documents/current`. Carries no body — the text is fetched on demand from the
 * per-type endpoint (`getDocument`).
 */
export interface LegalDocument {
  version: string;
  publishedAt: string;
  acceptedByMe: boolean;
}

export interface LegalDocumentsCurrent {
  terms: LegalDocument;
  privacy: LegalDocument;
}

/** The full document including its current text, from `GET /api/legal-documents/{type}` (BR8). */
export interface LegalDocumentDetail {
  type: LegalDocumentType;
  version: string;
  publishedAt: string;
  body: string;
}

/** Domain-oriented API for the legal documents (SPEC-0006, committed snapshot docs/api/openapi.json). */
@Injectable({ providedIn: 'root' })
export class LegalApi {
  private readonly http = inject(HttpClient);

  /** Version + publication date + my acceptance state per document (no body). */
  getCurrent(): Observable<LegalDocumentsCurrent> {
    return this.http.get<LegalDocumentsCurrent>('/api/legal-documents/current');
  }

  /** The full current document (with text) of a given type — used by the pages and the
   * acceptance screen to show the body. */
  getDocument(type: LegalDocumentType): Observable<LegalDocumentDetail> {
    return this.http.get<LegalDocumentDetail>(`/api/legal-documents/${type}`);
  }

  /** Records acceptance of `version` of `type` (BR8). The backend rejects an outdated version
   * with 409 legal.version-outdated, so the currently-displayed version is sent explicitly. */
  accept(type: LegalDocumentType, version: string): Observable<void> {
    return this.http.post<void>(`/api/legal-documents/${type}/accept`, { version });
  }
}
