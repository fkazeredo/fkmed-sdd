import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

/**
 * A Home banner (SPEC-0005 BR6): the server already filters by `active` and validity window
 * (start/end optional) and returns only the ones to display, in content-defined order.
 */
export interface HomeBanner {
  title: string;
  text: string;
  buttonLabel: string;
  destination: string;
  imageUrl: string | null;
  order: number;
}

/**
 * A Home notice (SPEC-0005 BR7): the server returns only the active ones, in content-defined
 * priority order. `severity` drives the visual distinction between `ALERT` and `INFORMATIVE`.
 */
export interface HomeNotice {
  title: string;
  severity: 'INFORMATIVE' | 'ALERT';
  body: string;
  order: number;
}

export interface HomeContentResponse {
  banners: HomeBanner[];
  notices: HomeNotice[];
}

/**
 * Contract of GET /api/content/home (committed snapshot: docs/api/openapi.json). BR8: the
 * caller MUST treat a failure of this endpoint as "no content" — it must not break the rest
 * of the Home screen (card + shortcuts keep working).
 */
@Injectable({ providedIn: 'root' })
export class HomeApi {
  private readonly http = inject(HttpClient);

  getHomeContent(): Observable<HomeContentResponse> {
    return this.http.get<HomeContentResponse>('/api/content/home');
  }
}
