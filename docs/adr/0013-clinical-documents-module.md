# ADR 0013: Module map revision — `domain.clinicaldocs` (Minha Saúde clinical documents)

## Status

Accepted

## Context

SPEC-0011 introduces the beneficiary's digital clinical documents (exam orders, referrals,
prescriptions, sick notes) — read-only for beneficiaries, born in telemedicine closures (SPEC-0010)
or from the operator (SPEC-0018), with per-type validity, immutability, filtered lists, type-specific
detail and faithful PDF. This is a new bounded context (document custody + read) that does not belong
to `domain.telemedicine` (the care session), `domain.appointment`, `domain.card` or `domain.plan`.
ADR-0001's "grow the map one spec at a time" policy is triggered.

## Decision

Add **`domain.clinicaldocs`** as the 10th verified Modulith module owning `clinical_document`
(type `EXAM_ORDER|REFERRAL|PRESCRIPTION|SICK_NOTE`, beneficiary, professional+CRM, issued_at,
`valid_until` nullable stamped at issue — DL-0019, immutable payload, origin session/operator ref)
plus the type-specific item tables (exam items + TUSS, prescription items, referral fields,
sick-note fields **including `cid`** — DL-0020). Public API is read-only `/api/clinical-documents*`
(list with beneficiary+period filters, detail, PDF). PDFs reuse the **OpenPDF** setup of `domain.card`
(ADR-0007). Documents are **created only** through an internal issuance facade the tele closure
(SPEC-0010 BR10) and the operator-sim (SPEC-0018) call — never via a beneficiary write path (BR8
immutability). Family scope + dependent-access audit reuse `BeneficiaryAccess`/audit (SPEC-0003).

## Consequences

- **Positive:** a single, immutable document store feeding Minha Saúde; read-only surface for
  beneficiaries; PDF and scope reuse proven patterns; issuance is a narrow internal seam.
- **Negative:** a 10th module raises the ModularityTest/diagram surface; the referral→scheduling
  handoff couples the FE to SPEC-0009's wizard (via a pre-selected specialty, not a backend cycle).

## Alternatives Considered

- Fold documents into `domain.telemedicine` — rejected: documents outlive and exist independently of
  tele sessions (operator-issued too); different owner/lifecycle.
- A beneficiary upload path — rejected: out of scope (documents are born in care/operator), and it
  would break immutability.

## Revision Triggers

- Lab results/full medical records (out of scope now) would extend the model.
- A second PDF-heavy module might promote a shared PDF utility out of `domain.card`.

## References

SPEC-0011 · SPEC-0010 (BR10 issuance) · SPEC-0018 (operator issuance) · ADR-0001 (module map) ·
ADR-0007 (OpenPDF) · DL-0019 (validity) · DL-0020 (CID) · DECISIONS-BASELINE §0019 ·
diagram `docs/architecture-diagrams/modules.puml`.
