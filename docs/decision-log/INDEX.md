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
| [0010](DL-0010-digital-card-plan-category-value.md) | 2026-07-05 | 2 / digital-card | 0007 | Medium | Cheap | Digital-card `planCategory` column and seeded value |
| [0011](DL-0011-profile-and-legal-documents-design.md) | 2026-07-05 | 2 / profile | 0006 | Medium | Cheap | Profile & legal-document design (module placement, code-space mapping, current-version source, UF no-cache) |
| [0012](DL-0012-provider-seals-parameterizable.md) | 2026-07-05 | 3 / network | 0008 | High | Cheap | Provider seals as parameterizable qualification badges (OQ1) |
| [0013](DL-0013-booking-antecedence-2h.md) | 2026-07-05 | 3 / appointments | 0009 | High | Cheap | Minimum booking antecedence = 2 hours (OQ1, owner-decided) |
| [0014](DL-0014-geography-registry-and-coverage.md) | 2026-07-05 | 3 / network | 0008 | High | Cheap | Full IBGE geography registry + plan coverage model (owner-decided) |
| [0015](DL-0015-medical-order-magic-byte-duplication.md) | 2026-07-05 | 3 / appointments | 0009 | Medium | Cheap | Duplicate the magic-byte upload check in domain.appointment |
| [0016](DL-0016-protocol-generator-placement.md) | 2026-07-05 | 3 / appointments | 0003, 0009 | Medium | Cheap | Protocol generator placement and format (domain.plan) |
| [0017](DL-0017-tele-disconnection-hold.md) | 2026-07-06 | 4 / telemedicine | 0010 | Medium | Cheap | Queue disconnection hold = 2 minutes (OQ2) |
| [0018](DL-0018-scheduled-teleconsultation-virtual-unit.md) | 2026-07-06 | 4 / telemedicine | 0010, 0009 | Medium | Cheap | Scheduled teleconsultation as a SPEC-0009 appointment on a virtual Telemedicina unit |
| [0019](DL-0019-clinical-document-validity-parameters.md) | 2026-07-06 | 4 / clinical-docs | 0011 | High | Cheap | Clinical-document validity stamped at issue (product parameters) |
| [0020](DL-0020-cid-displayed-on-sick-notes.md) | 2026-07-06 | 4 / clinical-docs | 0011 | High | Cheap | CID IS displayed on sick notes (owner-decided) |
| [0021](DL-0021-minimal-operator-sim-tele-slice.md) | 2026-07-06 | 4 / telemedicine | 0018, 0010 | High | Cheap | Only the telemedicine+documents slice of SPEC-0018 lands in Phase 4 (owner-decided) |
| [0022](DL-0022-sse-queue-transport-design.md) | 2026-07-06 | 4 / telemedicine | 0010 | Medium | Cheap | SSE queue transport — periodic server re-emit |
