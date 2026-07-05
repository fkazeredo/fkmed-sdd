import { Injectable, signal } from '@angular/core';

/**
 * Shared avatar state (SPEC-0006 BR3): the beneficiary photo is served by
 * `GET /api/beneficiaries/{id}/photo` (200 bytes / 404 = no photo), reached through each
 * beneficiary's `avatarUrl`. When a photo is uploaded or removed anywhere in the app, every place
 * the avatar appears (Home card, Perfil header, the Alterar Foto screen) must reflect it without a
 * new login. This service is that single source of truth: it holds a per-beneficiary override on
 * top of the URL the backend last reported, so consumers re-render immediately.
 *
 * - no override for an id → the caller's `fallback` (the backend `avatarUrl`) is used;
 * - after an upload → the photo endpoint with a cache-buster, so the `<img>` reloads the new bytes;
 * - after a removal → `null`, forcing the placeholder without hitting the (now 404) endpoint.
 */
@Injectable({ providedIn: 'root' })
export class AvatarStateService {
  private readonly overrides = signal<Record<string, string | null>>({});

  /** The avatar URL to render for a beneficiary: a local override (upload/removal) wins over the
   * backend fallback; when neither is known the placeholder is used (null). */
  resolve(beneficiaryId: string | undefined, fallback: string | null): string | null {
    if (!beneficiaryId) {
      return fallback;
    }
    const map = this.overrides();
    return beneficiaryId in map ? map[beneficiaryId] : fallback;
  }

  /** Records a fresh photo for the beneficiary (BR3): cache-busted so every bound `<img>` reloads. */
  onPhotoChanged(beneficiaryId: string): void {
    const url = `/api/beneficiaries/${beneficiaryId}/photo?v=${Date.now()}`;
    this.overrides.update((map) => ({ ...map, [beneficiaryId]: url }));
  }

  /** Records the removal (BR3): the placeholder shows everywhere immediately. */
  onPhotoRemoved(beneficiaryId: string): void {
    this.overrides.update((map) => ({ ...map, [beneficiaryId]: null }));
  }
}
