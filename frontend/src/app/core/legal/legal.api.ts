import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export type LegalDocumentType = 'TERMS' | 'PRIVACY';

/**
 * A versioned legal document and my acceptance state (SPEC-0006 BR8). `version` and `publishedAt`
 * are always present (frozen contract of `GET /api/legal-documents/current`); `body` (the current
 * text the page must show, BR8) is optional here because the frozen contract summary did not
 * enumerate it — see the note in `legal-documents.service.ts`. Rendered when present.
 */
export interface LegalDocument {
  version: string;
  publishedAt: string;
  acceptedByMe: boolean;
  body?: string;
}

export interface LegalDocumentsCurrent {
  terms: LegalDocument;
  privacy: LegalDocument;
}

/** Domain-oriented API for the legal documents (SPEC-0006, committed snapshot docs/api/openapi.json). */
@Injectable({ providedIn: 'root' })
export class LegalApi {
  private readonly http = inject(HttpClient);

  getCurrent(): Observable<LegalDocumentsCurrent> {
    return this.http.get<LegalDocumentsCurrent>('/api/legal-documents/current');
  }

  /** Records acceptance of the current version of `type` (BR8); outdated → 409 legal.version-outdated. */
  accept(type: LegalDocumentType): Observable<void> {
    return this.http.post<void>(`/api/legal-documents/${type}/accept`, {});
  }
}
