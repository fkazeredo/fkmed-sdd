import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Full profile of one beneficiary (SPEC-0006): contract data (read-only, BR4) plus the editable
 * contact/address fields (BR5). Frozen contract of `GET /api/beneficiaries/{id}/profile`
 * (committed snapshot: docs/api/openapi.json). `cpf` arrives masked from the server.
 */
export interface BeneficiaryProfile {
  fullName: string;
  cpf: string;
  birthDate: string;
  cardNumber: string;
  planName: string;
  contactEmail: string;
  mobile: string;
  landline: string;
  cep: string;
  street: string;
  number: string;
  complement: string;
  neighborhood: string;
  city: string;
  uf: string;
}

/** The editable subset (BR5) — a PATCH carries only the fields the user actually changed (BR7). */
export type ContactUpdate = Partial<
  Pick<
    BeneficiaryProfile,
    | 'contactEmail'
    | 'mobile'
    | 'landline'
    | 'cep'
    | 'street'
    | 'number'
    | 'complement'
    | 'neighborhood'
    | 'city'
    | 'uf'
  >
>;

/** Domain-oriented API of the Perfil area (SPEC-0006). */
@Injectable({ providedIn: 'root' })
export class ProfileApi {
  private readonly http = inject(HttpClient);

  getProfile(beneficiaryId: string): Observable<BeneficiaryProfile> {
    return this.http.get<BeneficiaryProfile>(`/api/beneficiaries/${beneficiaryId}/profile`);
  }

  /** Partial update (BR7): the body carries only changed fields. */
  updateContacts(beneficiaryId: string, changes: ContactUpdate): Observable<void> {
    return this.http.patch<void>(`/api/beneficiaries/${beneficiaryId}/contacts`, changes);
  }

  /** Multipart upload, form field `file` (BR2). */
  uploadPhoto(beneficiaryId: string, file: Blob): Observable<void> {
    const form = new FormData();
    form.append('file', file);
    return this.http.put<void>(`/api/beneficiaries/${beneficiaryId}/photo`, form);
  }

  removePhoto(beneficiaryId: string): Observable<void> {
    return this.http.delete<void>(`/api/beneficiaries/${beneficiaryId}/photo`);
  }
}
