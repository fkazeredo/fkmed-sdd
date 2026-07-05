import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { shareReplay, tap } from 'rxjs/operators';
import { LegalApi, LegalDocumentsCurrent, LegalDocumentType } from './legal.api';

/**
 * Shared legal-documents state (SPEC-0006 BR8). Both the interception guard and the legal pages
 * read the single cached `current` snapshot. The snapshot is loaded once per session (the guard's
 * first navigation triggers it) and mutated in place as the user accepts a version, so
 * `hasPending()` flips to false the moment acceptance succeeds and normal navigation resumes.
 *
 * NOTE (contract): the frozen `GET /api/legal-documents/current` summary lists only
 * `{version, publishedAt, acceptedByMe}` per document, but BR8 requires the pages to render the
 * current *text*; `LegalDocument.body` is therefore optional and rendered when the server provides
 * it. Flagged to the architect as a contract point to re-sync against the real OpenAPI snapshot.
 */
@Injectable({ providedIn: 'root' })
export class LegalDocumentsService {
  private readonly api = inject(LegalApi);

  readonly current = signal<LegalDocumentsCurrent | null>(null);
  private inFlight: Observable<LegalDocumentsCurrent> | undefined;

  /** Loads the snapshot once and caches it; concurrent guard calls share the single request. */
  ensureLoaded(): Observable<LegalDocumentsCurrent> {
    const cached = this.current();
    if (cached) {
      return of(cached);
    }
    if (!this.inFlight) {
      this.inFlight = this.api.getCurrent().pipe(
        tap((snapshot) => {
          this.current.set(snapshot);
          this.inFlight = undefined;
        }),
        shareReplay(1),
      );
    }
    return this.inFlight;
  }

  /** Drops the cache and re-fetches — used after a 409 (a newer version was published meanwhile). */
  reload(): Observable<LegalDocumentsCurrent> {
    this.current.set(null);
    this.inFlight = undefined;
    return this.ensureLoaded();
  }

  /** The mandatory documents still awaiting the user's acceptance (BR8). */
  readonly pendingTypes = computed<LegalDocumentType[]>(() => {
    const snapshot = this.current();
    if (!snapshot) {
      return [];
    }
    const pending: LegalDocumentType[] = [];
    if (!snapshot.terms.acceptedByMe) {
      pending.push('TERMS');
    }
    if (!snapshot.privacy.acceptedByMe) {
      pending.push('PRIVACY');
    }
    return pending;
  });

  readonly hasPending = computed(() => this.pendingTypes().length > 0);

  /** Flips the local acceptance flag after a successful POST accept (BR8). */
  markAccepted(type: LegalDocumentType): void {
    const snapshot = this.current();
    if (!snapshot) {
      return;
    }
    this.current.set(
      type === 'TERMS'
        ? { ...snapshot, terms: { ...snapshot.terms, acceptedByMe: true } }
        : { ...snapshot, privacy: { ...snapshot.privacy, acceptedByMe: true } },
    );
  }
}
