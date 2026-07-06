# 0011 - Clinical Documents (Minha Saúde)

**Status:** Approved (Phase 4)

## Goal

One place for the beneficiary's digital clinical documents — exam orders, referrals,
prescriptions and sick notes — issued in care sessions (mainly telemedicine) or by the
operator, with consultation, validity control and faithful PDF download.

## Scope

- Minha Saúde hub with 3 categories (exam orders · referrals · prescriptions/sick notes).
- Lists with combined filters (beneficiary + period) and validity badges.
- Type-specific detail; "Agendar consulta" action from referrals.
- PDF download; immutability; audit on dependent access.

## Business Context

Documents are born in telemedicine closures (SPEC-0010 BR10) or loaded by the operator
(SPEC-0018 can issue them in the POC). Beneficiaries never upload documents here. Each
document type has a default validity that is a product parameter.

## Business Rules

- **BR1** — The hub MUST give access to the 3 categories: **Solicitação de exames** (exam
  orders), **Encaminhamentos** (referrals) and **Receituários/Atestados**
  (prescriptions/sick notes). Item counters per category *(MAY)*.
- **BR2** — Lists MUST apply combined filters — beneficiary (accessible ones, default
  "todos") and period (30/90/365 days or custom range) — ordered most recent first. Card:
  document title, issuing professional + CRM, issue date, beneficiary, validity badge
  ("Válido até dd/mm/aaaa" or "Expirado").
- **BR3** — Documents issued by telemedicine MUST appear immediately after the session
  closes.
- **BR4** — Default validities (product parameters, adjustable): prescription **30 days**
  from issue; exam order **90 days**; referral **90 days**; sick note **no validity**.
- **BR5** — Expired documents MUST remain visible with the "Expirado" badge and remain
  downloadable.
- **BR6** — Detail MUST show the common header (type, issue date, professional + CRM,
  beneficiary, validity) plus the type-specific body:
  - *Exam order*: requested exams (name + TUSS code) and clinical indication.
  - *Referral*: target specialty and reason; button **"Agendar consulta"** opening the
    SPEC-0009 wizard with the specialty pre-selected.
  - *Prescription*: medications with posology and guidance.
  - *Sick note*: leave period, the **CID (diagnosis code)** and notes (owner decision — the CID
    IS displayed on sick notes, DL-0020).
- **BR7** — "Baixar PDF" MUST generate a PDF faithful to the detail content, including
  issuer, CRM, dates and validity.
- **BR8** — A document belongs to exactly **one** beneficiary and is **immutable** after
  issue: corrections generate a new document; the previous one stays in history.
- **BR9** — The "todos" filter aggregates only beneficiaries accessible to the user; a
  titular accessing a dependent's document generates an audit entry (SPEC-0003 BR6); a
  dependent never sees third-party documents.

## Input/Output Examples

- `GET /api/clinical-documents?category=PRESCRIPTION&beneficiaryId=all&period=P30D` →
  `200 {"items":[{"id":"…","type":"PRESCRIPTION","professional":"Dra. …","crm":"CRM 12345
  RJ","issuedAt":"2026-07-04","beneficiary":"PEDRO","validUntil":"2026-08-03",
  "expired":false}]}`.
- `GET /api/clinical-documents/{id}` of another family's document → `404
  {"code":"document.not-found"}` (error case — existence not revealed).

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/clinical-documents` | Filtered lists per category |
| GET | `/api/clinical-documents/{id}` | Type-specific detail |
| GET | `/api/clinical-documents/{id}/pdf` | PDF download |

## Events

Consumes issuance (SPEC-0010/SPEC-0018). `ClinicalDocumentIssued` → SPEC-0004 (type,
beneficiary, link) — already triggered by the issuing flow.

## Persistence Changes

Migration (number at implementation): `clinical_document` (id, type
`EXAM_ORDER|REFERRAL|PRESCRIPTION|SICK_NOTE`, beneficiary_id, professional_name, crm,
issued_at, valid_until nullable, origin session/operator ref, immutable payload);
`exam_order_item` (exam name, TUSS); `prescription_item` (medication, posology, guidance);
referral fields (target specialty code, reason); sick-note fields (period, **cid**, notes).
Validity defaults as configuration parameters.

## Validation Rules

Read-only module for beneficiaries; filters validated (period range sane, beneficiary in
scope — SPEC-0003 BR3).

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Document not found / out of scope | `document.not-found` | 404 |

## Observability Requirements

Audit on dependent-document access (BR9). Counter of PDFs downloaded per type. No clinical
content in logs.

## Tests Required

- **Domain/unit:** validity computation per type; expiry boundary (day 30/90).
- **Integration (Testcontainers):** filters; immediate visibility after tele closure;
  immutability (no update path); scope + audit.
- **API contract:** all endpoints.
- **Frontend unit:** filter behavior; badges; referral pre-selection handoff.
- **E2E:** tele-issued prescription appears and downloads; referral opens scheduling with
  specialty pre-selected.

## Acceptance Criteria

- **AC1** (BR3, BR4) — Given a teleconsultation closes issuing a prescription, then it
  appears at the top of Receituários/Atestados with a 30-day validity.
- **AC2** (BR5) — Given a prescription older than 30 days, then it shows "Expirado" and
  still allows PDF download.
- **AC3** (BR9) — Given the filter beneficiary = PEDRO, then only PEDRO's documents are
  listed; MARIA's access is audit-logged.
- **AC4** (BR6) — Given a referral to Cardiologia, when I tap "Agendar consulta", then the
  wizard opens with Cardiologia pre-selected.
- **AC5** (BR7) — Given the download of an exam order, then the PDF contains professional,
  CRM, date, validity and the exam list with TUSS codes.
- **AC6** (BR2) — Given period "últimos 30 dias", then older documents do not appear.

## Resolved Decisions (Phase 4 — owner)

- **OQ1 → CID IS displayed on sick notes** (owner, overriding the spec's proposed
  "not displayed" default; DL-0020). The sick-note detail and its PDF show the CID
  (diagnosis code). *Product accepts the medical-privacy trade-off; revisit with legal
  if the product position changes.*

## Out of Scope

Lab results/reports; full medical records; beneficiary uploads (documents are born in care
sessions or from the operator); document sharing links.
