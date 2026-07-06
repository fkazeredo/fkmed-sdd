# DL-0016 — Protocol generator placement and format (SPEC-0003 BR9 · first used by SPEC-0009)

- **Phase/slice:** Phase 3 · Appointments (SPEC-0009), implementing SPEC-0003 BR9
- **Spec(s):** SPEC-0003 (BR9; §Persistence lines 88–98), SPEC-0009 (BR7)
- **Related ADR:** ADR-0012 (domain.appointment)
- **Date:** 2026-07-05
- **Status:** ASSUMED
- **Confidence:** Medium
- **Reversibility:** Cheap

## Gap

SPEC-0003 BR9 defines a unique protocol (`^[A-Z]{2}-\d{8}-\d{4}$`, e.g. `AG-AAAAMMDD-####`) but its
implementation was explicitly deferred to Phase 3 (owner, recorded in the slice-1.3 conclusion
report). SPEC-0009 is the **first consumer** (`AG-`). Where does the generator live, and is it
built generic (future `RE-` reimbursement, `PV-` preview) or appointment-specific?

## Decision

Build the generator in **`domain.plan`** (the module that already implements SPEC-0003's
beneficiary-context/authorization rules, of which BR9 is one) as a **public service** taking a
2-letter prefix. Backing table `protocol_sequence` (prefix, date, counter) with an **atomic
increment** (per-prefix, per-day counter; unique protocol); format enforced as `^[A-Z]{2}-\d{8}-
\d{4}$`. `domain.appointment` (which already depends on `domain.plan` for beneficiary scope) calls
it with prefix `AG-`. Generic in shape but **not** over-abstracted — one consumer today (Rule Zero).

## Justification

BR9 is a SPEC-0003 rule ⇒ its home is the module that owns SPEC-0003. A single shared service +
table avoids a per-spec re-implementation when `RE-`/`PV-` arrive, without building a framework
ahead of need. Atomic increment guarantees uniqueness under concurrency (the same concern as slot
capacity — see DL-0005 precedent).

## Alternatives discarded

- Generator inside `domain.appointment` — rejected: BR9 is not appointment-specific; the next
  consumer would duplicate it.
- A new shared module just for protocols — rejected (premature; `domain.plan` already fits).

## Impact / How to revert

`protocol_sequence` table + a `domain.plan` service. Revert/relocate if a cleaner shared home
emerges when the 2nd consumer lands.
