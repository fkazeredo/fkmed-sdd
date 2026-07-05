# Decision Log — Index

Autonomous decisions (gaps and Open Questions resolved under explicitly authorized
autonomy, without the owner in the room). Append-only; format in
[`docs/RUN-PHASE.md`](../RUN-PHASE.md). A revised decision gets a NEW DL referencing the
old one.

## ⚠️ Highlights — Reversibility=Costly or Confidence=Low

| DL | Title | Why highlighted |
|---|---|---|
| — | *(none)* | |

## All decisions

| DL | Date | Phase/Slice | Spec(s) | Confidence | Reversibility | Title |
|---|---|---|---|---|---|---|
| [0001](DL-0001-first-access-contract-shape.md) | 2026-07-04 | 1 / 1.1 | 0002 | Medium | Cheap | First-access contract shape (acceptance, registration token, resend neutrality) |
| [0002](DL-0002-lockout-counter-and-window-semantics.md) | 2026-07-04 | 1 / 1.2 | 0002 | Medium | Cheap | Account-lockout counter and window semantics (BR8 edge cases) |
| [0003](DL-0003-recovery-and-password-change-shape.md) | 2026-07-04 | 1 / 1.2 | 0002 | Medium | Cheap | Recovery / reset / password-change event and audit shape |
| [0004](DL-0004-beneficiary-context-authorization-placement.md) | 2026-07-04 | 1 / 1.3 | 0003 | Medium | Cheap | Beneficiary context & family-scope authorization placement and contract |
| [0005](DL-0005-concurrent-account-update-translation.md) | 2026-07-04 | 1 / 1.3 | 0002 | Medium | Cheap | Concurrent account-update translation (optimistic lock, débito técnico A) |
| [0006](DL-0006-notification-recipient-beneficiary-without-account.md) | 2026-07-05 | 2 / notifications | 0004 | Medium | Cheap | Notification recipient for a beneficiary without an account (OQ1) |
| [0007](DL-0007-in-app-notification-retention.md) | 2026-07-05 | 2 / notifications | 0004 | Medium | Cheap | In-app notification retention window (OQ2) |
| [0008](DL-0008-phase2-notification-centralization-scope.md) | 2026-07-05 | 2 / notifications | 0004, 0006 | Medium | Cheap | Phase-2 notification centralization scope |
| [0009](DL-0009-digital-card-pdf-layout.md) | 2026-07-05 | 2 / digital-card | 0007 | High | Cheap | Digital-card PDF layout (OQ1) |
