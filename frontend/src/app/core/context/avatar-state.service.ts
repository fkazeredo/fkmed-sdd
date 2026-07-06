import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';

/**
 * Shared avatar state (SPEC-0006 BR3). The beneficiary photo is served by
 * `GET /api/beneficiaries/{id}/photo` under `/api/**` with a mandatory Bearer token, so it CANNOT
 * be loaded via a plain `<img src>` (the OIDC interceptor only signs XHR/fetch, not `<img>`
 * requests). This service blob-fetches the photo through HttpClient (the interceptor injects the
 * token), turns it into an object URL and exposes it as a signal — so every place the avatar
 * appears (Home card, Perfil header, Alterar Foto) renders the same authenticated bytes and updates
 * without a new login.
 *
 * - `load` fetches the photo blob; 404 (no photo) → null → placeholder.
 * - `setFromBlob` reuses the bytes we already hold after an upload (no extra round-trip).
 * - `remove` forces the placeholder.
 * - object URLs are revoked whenever the entry for a beneficiary is replaced, so repeated photo
 *   changes never leak.
 */
@Injectable({ providedIn: 'root' })
export class AvatarStateService {
  private readonly http = inject(HttpClient);

  /** Per-beneficiary object URL: a string when a photo is loaded, null when there is none. */
  private readonly urls = signal<Record<string, string | null>>({});

  /** The object URL to render for a beneficiary, or null for the placeholder. */
  avatarUrl(beneficiaryId: string | undefined): string | null {
    if (!beneficiaryId) {
      return null;
    }
    const map = this.urls();
    return beneficiaryId in map ? map[beneficiaryId] : null;
  }

  /** Blob-fetches the beneficiary's photo (Bearer added by the interceptor); 404 → placeholder. */
  load(beneficiaryId: string): void {
    this.http.get(`/api/beneficiaries/${beneficiaryId}/photo`, { responseType: 'blob' }).subscribe({
      next: (blob) => this.setFromBlob(beneficiaryId, blob),
      error: () => this.setNull(beneficiaryId),
    });
  }

  /** Publishes a photo we already hold (the just-uploaded/cropped blob) — no extra fetch (BR3). */
  setFromBlob(beneficiaryId: string, blob: Blob): void {
    const url = this.createObjectUrl(blob);
    this.revoke(beneficiaryId);
    this.urls.update((map) => ({ ...map, [beneficiaryId]: url }));
  }

  /** Records a removal (BR3): the placeholder shows everywhere immediately. */
  remove(beneficiaryId: string): void {
    this.setNull(beneficiaryId);
  }

  private setNull(beneficiaryId: string): void {
    this.revoke(beneficiaryId);
    this.urls.update((map) => ({ ...map, [beneficiaryId]: null }));
  }

  private revoke(beneficiaryId: string): void {
    const existing = this.urls()[beneficiaryId];
    if (existing) {
      try {
        URL.revokeObjectURL(existing);
      } catch {
        // environment (jsdom) without object-URL support — nothing to revoke
      }
    }
  }

  private createObjectUrl(blob: Blob): string | null {
    try {
      return URL.createObjectURL(blob);
    } catch {
      return null;
    }
  }
}
