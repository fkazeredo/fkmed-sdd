import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * Contracts of `/api/network/*` reconciled to the REAL backend (OpenAPI snapshot, integration
 * rework). The funnel lists (`states`, `municipalities`, `neighborhoods`, `service-types`,
 * `specialties`) return **raw arrays** — there is no `{items:[…]}` envelope; only the provider
 * search responses carry `{referenceDate, items:[…]}`.
 *
 * - `states`/`municipalities`/`neighborhoods` → `string[]` (UF codes / municipality names /
 *   neighborhood names). The string IS the value used to filter (`uf=`, `municipality=`,
 *   `neighborhood=`) and, for this single-locale product, the label rendered as-is.
 * - `service-types` → carries `hasSpecialtyStep` per type; the frontend uses that flag to decide
 *   the specialty step (BR5) — no hardcoded service-type code.
 * - Provider cards carry `locality` as a **single pre-formatted string** (e.g.
 *   `"CENTRO, RIO DE JANEIRO – RJ"`), not separate neighborhood/municipality/uf fields.
 */
export interface ServiceTypeOption {
  code: string;
  name: string;
  /** BR5: only types with `hasSpecialtyStep=true` show the specialty step; others skip to results. */
  hasSpecialtyStep: boolean;
}

/** `specialties` shape (BR6, registry data). */
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

/** BR7/BR8 card fields — shared by the funnel results and the name-search results. `locality` is a
 * single already-formatted string ("BAIRRO, MUNICÍPIO – UF"). */
export interface ProviderCard {
  id: string;
  name: string;
  locality: string;
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

/**
 * Domain-oriented API of the Provider Network Search feature (SPEC-0008). No raw HttpClient in
 * components (frontend-angular.md §HTTP and errors).
 */
@Injectable({ providedIn: 'root' })
export class NetworkApi {
  private readonly http = inject(HttpClient);

  /** BR4: only UFs within the plan's coverage (a state-wide RJ plan → `["RJ"]`). */
  getStates(): Observable<string[]> {
    return this.http.get<string[]>('/api/network/states');
  }

  /** BR2: `q` drives the server-side real-time filter as the user types. */
  getMunicipalities(uf: string, q?: string): Observable<string[]> {
    let params = new HttpParams().set('uf', uf);
    if (q) {
      params = params.set('q', q);
    }
    return this.http.get<string[]>('/api/network/municipalities', { params });
  }

  getNeighborhoods(uf: string, municipality: string): Observable<string[]> {
    const params = new HttpParams().set('uf', uf).set('municipality', municipality);
    return this.http.get<string[]>('/api/network/neighborhoods', { params });
  }

  getServiceTypes(): Observable<ServiceTypeOption[]> {
    return this.http.get<ServiceTypeOption[]>('/api/network/service-types');
  }

  getSpecialties(): Observable<RegistryOption[]> {
    return this.http.get<RegistryOption[]>('/api/network/specialties');
  }

  /** BR7/BR9: `neighborhood` omitted means "Todos" (whole municipality); `specialty` only applies
   * when the chosen service type has a specialty step (BR5). */
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
