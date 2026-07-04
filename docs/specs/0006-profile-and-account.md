# 0006 - Profile and Account

**Status:** Draft

## Goal

The user's area: personalize the avatar photo, keep contact data current (a prerequisite
for reimbursement and notifications), read and accept versioned legal documents, reach
security/service shortcuts, and sign out — with contract data protected as read-only.

## Scope

- Profile menu (fixed item order) with product version display.
- Photo upload (square crop), replace and remove — per beneficiary.
- Registration editing: contacts and address (contract data read-only).
- Terms of Use and Privacy Notice pages; versioned mandatory re-acceptance flow.
- Sign out with confirmation.

## Business Context

Contract data (name, CPF, birth date, card number, plan) belongs to the operator and can
only be changed through service channels. Contact data belongs to the beneficiary and
feeds notifications (SPEC-0004) and the reimbursement contact gate (SPEC-0015). Legal
documents are versioned; using the portal requires having accepted the current versions
(SPEC-0002 BR15 covers first acceptance at registration).

## Business Rules

- **BR1** — The profile menu MUST present, in order: header card ("Olá, {NOME}" + plan +
  card number), then **Alterar Foto**, **Segurança** (→ SPEC-0002), **Alterar Cadastro**,
  **Central de Libras** (→ SPEC-0014), **Perguntas Frequentes** (→ SPEC-0014),
  **Comunicado de privacidade**, **Termos de uso**, **Sair** (with the product version
  right-aligned), and the expandable LGPD item (same notice component as Home).
- **BR2** — Photo upload MUST accept only JPG/PNG up to **5 MB**, validating the real file
  content (magic bytes), never just the extension; invalid files are refused with a clear
  error and nothing is saved. Upload flows through preview with square crop.
- **BR3** — The photo belongs to the **beneficiary**: a titular MAY set/remove a
  dependent's photo via the active-beneficiary selector; a dependent only their own
  (SPEC-0003 BR1). Changes reflect everywhere the avatar appears without a new login.
  "Remover foto" returns to the placeholder.
- **BR4** — Contract data (full name, CPF masked, birth date, card number) MUST be
  displayed read-only with the hint "Para alterar, procure os canais de atendimento".
- **BR5** — Editable fields: contact e-mail, mobile phone, landline (optional), CEP,
  street, number, complement, neighborhood, city, UF.
- **BR6** — Contact e-mail and mobile are **mandatory and can never be emptied** — they are
  prerequisites for reimbursement requests (SPEC-0015) and notification targets. The
  contact e-mail is independent from the login e-mail (SPEC-0002).
- **BR7** — Saving MUST be partial (only changed fields), show confirmation, persist across
  reopening, and be audited (SPEC-0003 BR6).
- **BR8** — Legal document pages MUST show the current text with **version number and
  publication date**. When a new version exists that the user has not accepted, the next
  access MUST be intercepted by the document screen with "Li e aceito" before any internal
  navigation (only Sair remains available). Each acceptance is immutable, recorded with
  version, timestamp and user; history is preserved.
- **BR9** — Sair MUST ask for confirmation and immediately terminate the current session.
- **BR10** — The displayed product version MUST come from build configuration (never
  hardcoded).

## Input/Output Examples

- `PATCH /api/beneficiaries/{id}/contacts` `{"mobile":"(21) 99999-1234"}` → `200` (partial
  save; other fields untouched).
- Same call with `{"mobile":""}` → `422 {"code":"profile.mobile-required"}` (error case).
- Photo upload of an executable renamed `.png` → `422 {"code":"profile.photo-invalid-content"}`
  (error case).
- `GET /api/legal-documents/current` → `200 {"terms":{"version":"2.0","publishedAt":"…",
  "acceptedByMe":false}, "privacy":{…}}` → frontend intercepts navigation.

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/beneficiaries/{id}/profile` | Read-only contract data + editable contacts |
| PATCH | `/api/beneficiaries/{id}/contacts` | Partial update of contacts/address |
| PUT / DELETE | `/api/beneficiaries/{id}/photo` | Upload (multipart) / remove photo |
| GET | `/api/legal-documents/current` | Current terms/privacy + my acceptance state |
| POST | `/api/legal-documents/{type}/accept` | Record acceptance of current version |

## Events

`ContactDataChanged` (AFTER_COMMIT) → SPEC-0004 sends the security notice to the **old and
new** e-mail addresses + in-app (mandatory type).

## Persistence Changes

Migration (number at implementation): add contact/address columns (or `beneficiary_contact`
table) per BR5; `beneficiary_photo` (beneficiary_id, image, content_type, updated_at);
`legal_document` (type `TERMS|PRIVACY`, version, published_at, body) + seed of current
versions; `term_acceptance` shared with SPEC-0002. UF validated against a seeded UF
registry (baseline §0019).

## Validation Rules

Contact e-mail: valid format, mandatory. Mobile: `(99) 99999-9999`, mandatory. Landline:
`(99) 9999-9999`, optional. CEP: 8 digits. Street ≤ 120, number ≤ 10, complement ≤ 60,
neighborhood/city ≤ 80, UF in registry. Photo: JPG/PNG, ≤ 5 MB, content-validated.

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Photo wrong type/content | `profile.photo-invalid-content` | 422 |
| Photo over 5 MB | `profile.photo-too-large` | 422 |
| Mobile emptied/invalid | `profile.mobile-required` / `profile.mobile-invalid` | 422 |
| Contact e-mail emptied/invalid | `profile.contact-email-required` / `profile.contact-email-invalid` | 422 |
| CEP/UF invalid | `profile.cep-invalid` / `profile.uf-invalid` | 422 |
| Acceptance of outdated version | `legal.version-outdated` | 409 |

## Observability Requirements

Audit entries for contact changes, photo changes and term acceptances (SPEC-0003). No
photo bytes in logs. Counter of interception screens shown (adoption of new terms).

## Tests Required

- **Domain/unit:** partial-update semantics; mandatory-field guards; content-type sniffing.
- **Integration (Testcontainers):** photo upload/removal; contact change persists + audit +
  event; acceptance versioning and interception state.
- **API contract:** all endpoints and error codes.
- **Frontend unit:** field-level validation messages; navigation interception until
  acceptance; avatar propagation.
- **E2E:** edit contacts journey; new-terms interception journey; sign out.

## Acceptance Criteria

- **AC1** (BR6) — Given an invalid mobile format, then the error shows next to the field
  and saving stays blocked (error case).
- **AC2** (BR7) — Given a new contact e-mail saved, when I reopen Alterar Cadastro, then
  the new value is there and the audit trail recorded the change.
- **AC3** (BR2) — Given an executable renamed to `.png`, then the upload is refused with
  the invalid-file message (error case).
- **AC4** (BR3) — Given MARIA uploads a valid photo with PEDRO active, then PEDRO's avatar
  updates; given PEDRO tries to edit MARIA's contacts, then access is denied.
- **AC5** (BR8) — Given a new Terms version was published, when the user next accesses the
  portal, then navigation is blocked until "Li e aceito"; the acceptance records the new
  version.
- **AC6** (BR9) — Given Sair confirmed, then the session ends and any internal screen
  requires login again.
- **AC7** (BR6) — Given an attempt to clear the mobile field, then saving is refused with
  the mandatory-field message (error case).

## Open Questions

*(none — photo moderation was explicitly deferred)*

## Out of Scope

Changing contract data via portal; changing the login e-mail; photo moderation/reporting
(future); marketing communication preferences (notification preferences live in SPEC-0004).
