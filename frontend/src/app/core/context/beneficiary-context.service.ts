import { inject, Injectable, signal } from '@angular/core';
import { AccessibleBeneficiariesApi, AccessibleBeneficiary } from './accessible-beneficiaries.api';

/** sessionStorage key for the chosen active beneficiary — tab-scoped, never cross-tab (see
 * `core/auth/return-url.ts` for the same rationale applied to the return-route). */
export const ACTIVE_BENEFICIARY_KEY = 'fkmed.activeBeneficiary';

/**
 * The active-beneficiary context every screen in the shell operates on (SPEC-0003 BR5). Populated
 * from the accessible-beneficiaries API, which is already scoped server-side (BR1) — the active
 * beneficiary kept here is client-side convenience only; the server re-validates the target
 * beneficiary of every request against the caller's scope regardless (BR3).
 *
 * Default active beneficiary: the TITULAR (or the first entry when there is none — a dependent's
 * own session only ever lists themselves). A previously chosen id restores from `sessionStorage`
 * across reloads within the same tab, as long as it is still in the freshly loaded accessible list.
 */
@Injectable({ providedIn: 'root' })
export class BeneficiaryContextService {
  private readonly api = inject(AccessibleBeneficiariesApi);

  readonly accessible = signal<AccessibleBeneficiary[]>([]);
  readonly active = signal<AccessibleBeneficiary | null>(null);

  load(): void {
    this.api.getAccessibleBeneficiaries().subscribe((list) => {
      this.accessible.set(list);
      const savedId = sessionStorage.getItem(ACTIVE_BENEFICIARY_KEY);
      const restored = list.find((beneficiary) => beneficiary.beneficiaryId === savedId);
      const fallback = list.find((beneficiary) => beneficiary.role === 'TITULAR') ?? list[0] ?? null;
      this.active.set(restored ?? fallback);
    });
  }

  /** Switches the active beneficiary; ignores an id outside the accessible list (defensive). */
  setActive(beneficiaryId: string): void {
    const found = this.accessible().find((beneficiary) => beneficiary.beneficiaryId === beneficiaryId);
    if (!found) {
      return;
    }
    this.active.set(found);
    sessionStorage.setItem(ACTIVE_BENEFICIARY_KEY, beneficiaryId);
  }
}
