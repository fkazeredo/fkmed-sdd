import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Contracts of `/api/network/*` (SPEC-0008 §API Contracts, frozen — backend lands separately in
 * this parallel phase). Registry-coded lists (`states`, `service-types`, `specialties`) return a
 * `{code, name}` pair: `code` is the stable value used to filter (`uf=`, `serviceType=`,
 * `specialty=`), `name` is the ready-to-render pt-BR label (this app has a single locale — no
 * further i18n lookup needed, unlike the `carteirinha.coverage.*`-style enum labels). Municipality
 * and neighborhood are free-text registry data (persistence §Migration V15) — the name itself IS
 * the filter value, so those lists are plain `string[]`. Provider list/detail fields
 * (`name`/`neighborhood`/`municipality`/`uf`/`serviceType`/`specialties`) are already
 * display-ready strings from the backend, not codes needing a further lookup.
 */
export interface UfOption {
  code: string;
  name: string;
}

/** `service-types` and `specialties` share this shape (BR5/BR6, registry data). */
export interface RegistryOption {
  code: string;
  name: string;
}

/** BR14: seals are registry data (code, name, parameterizable description), embedded per provider. */
export interface Seal {
  code: string;
  name: string;
  description: string;
}

/** BR7/BR8 card fields — shared by the funnel results and the name-search results. */
export interface ProviderCard {
  id: string;
  name: string;
  neighborhood: string;
  municipality: string;
  uf: string;
  serviceType: string;
  seals: Seal[];
}

/** BR7: results always carry the reference date of the query alongside the items. */
export interface ProviderSearchResult {
  referenceDate: string;
  items: ProviderCard[];
}

/** BR12 full address — persistence §Migration V15 (CEP, street, number, complement). */
export interface ProviderAddress {
  cep: string;
  street: string;
  number: string;
  complement: string | null;
  neighborhood: string;
  municipality: string;
  uf: string;
}

/** BR12 provider detail. */
export interface ProviderDetail {
  id: string;
  name: string;
  serviceType: string;
  specialties: string[];
  address: ProviderAddress;
  phone: string;
  seals: Seal[];
}

export interface ProviderListFilters {
  uf: string;
  municipality: string;
  neighborhood?: string;
  serviceType: string;
  specialty?: string;
}

/** Registry code for "Consultórios–Clínicas–Terapias" (BR5) — the only service type with a
 * specialty step; every other type skips straight to results. Matches the code literally used in
 * SPEC-0008's own example query (`serviceType=CONSULTORIOS`). */
export const CONSULTORIOS_SERVICE_TYPE_CODE = 'CONSULTORIOS';

/**
 * Domain-oriented API of the Provider Network Search feature (SPEC-0008). No raw HttpClient in
 * components (frontend-angular.md §HTTP and errors).
 */
@Injectable({ providedIn: 'root' })
export class NetworkApi {
  private readonly http = inject(HttpClient);

  getStates(): Observable<{ items: UfOption[] }> {
    return this.http.get<{ items: UfOption[] }>('/api/network/states');
  }

  /** BR2: `q` drives the server-side real-time filter as the user types. */
  getMunicipalities(uf: string, q?: string): Observable<{ items: string[] }> {
    let params = new HttpParams().set('uf', uf);
    if (q) {
      params = params.set('q', q);
    }
    return this.http.get<{ items: string[] }>('/api/network/municipalities', { params });
  }

  getNeighborhoods(uf: string, municipality: string): Observable<{ items: string[] }> {
    const params = new HttpParams().set('uf', uf).set('municipality', municipality);
    return this.http.get<{ items: string[] }>('/api/network/neighborhoods', { params });
  }

  getServiceTypes(): Observable<{ items: RegistryOption[] }> {
    return this.http.get<{ items: RegistryOption[] }>('/api/network/service-types');
  }

  getSpecialties(): Observable<{ items: RegistryOption[] }> {
    return this.http.get<{ items: RegistryOption[] }>('/api/network/specialties');
  }

  /** BR7/BR9: `neighborhood` omitted means "Todos" (whole municipality); `specialty` only applies
   * when `serviceType` is CONSULTORIOS (BR5). */
  getProviders(filters: ProviderListFilters): Observable<ProviderSearchResult> {
    let params = new HttpParams()
      .set('uf', filters.uf)
      .set('municipality', filters.municipality)
      .set('serviceType', filters.serviceType);
    if (filters.neighborhood) {
      params = params.set('neighborhood', filters.neighborhood);
    }
    if (filters.specialty) {
      params = params.set('specialty', filters.specialty);
    }
    return this.http.get<ProviderSearchResult>('/api/network/providers', { params });
  }

  /** BR8: name search, ≥ 3 chars (else `422 network.query-too-short`), optional municipality filter. */
  searchProvidersByName(name: string, municipality?: string): Observable<ProviderSearchResult> {
    let params = new HttpParams().set('name', name);
    if (municipality) {
      params = params.set('municipality', municipality);
    }
    return this.http.get<ProviderSearchResult>('/api/network/providers/search', { params });
  }

  /** BR13: an inactive/unknown provider answers `410 network.provider-unavailable`. */
  getProvider(id: string): Observable<ProviderDetail> {
    return this.http.get<ProviderDetail>(`/api/network/providers/${id}`);
  }
}
