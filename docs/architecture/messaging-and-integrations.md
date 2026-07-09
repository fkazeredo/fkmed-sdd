# Messaging, Integrations, Files, Notifications and AI

> Read when: working with events, jobs/schedulers, idempotency, external APIs, uploads/imports,
> notifications or AI/ML components.

## Domain Events

Represent meaningful business facts explicitly: `AppointmentConfirmed`, `GuideStatusChanged`,
`ReimbursementStatusChanged`, `ClinicalDocumentIssued`.

FKMed currently uses in-process Spring application events. There is no external broker. Listener
patterns that matter here:

- do not roll back the business transaction because best-effort notification delivery failed;
- use `REQUIRES_NEW` where a side effect must persist independently;
- keep event payloads stable and free of unnecessary sensitive data;
- include masked data when the recipient copy needs it.

If events ever leave the process, introduce an outbox/inbox design, event ids, versioning, retries,
dead-letter/quarantine and replay tooling through a dedicated spec/ADR.

## Jobs

Simple jobs may use `@Scheduled`. Important jobs must consider idempotency, locking, timeout,
history, metrics, logs, safe restart and partial-failure recovery. Critical jobs need runbooks and
tests.

The current code has an audit-retention job; backup/restore automation is a production-readiness
gap and is not implemented in this slice.

## Idempotency

Add idempotency where duplicate execution causes damage: payment-like transitions, notification
dispatch, imports, external callbacks, jobs and double-submit operations. Prefer database
constraints/state checks before complex infrastructure.

Reimbursement submission uses an idempotency key because duplicate requests can create duplicate
protocols.

## External Integrations

External systems are unreliable and must not shape the domain. Put provider-specific DTOs, errors,
authentication and retry behavior behind an adapter or port. Every external call needs a timeout.
Retries must be deliberate and safe for the operation.

Current concrete integrations are intentionally small:

- e-mail through `EmailSender` with SMTP/logging implementations;
- embedded OIDC/OAuth2 endpoints;
- operator-simulation APIs for dev/E2E only;
- browser/download surfaces for PDFs;
- no NFS-e, ERP, payment gateway, SMS, WhatsApp API or external broker is implemented.

## Files and Uploads

Uploads are a security boundary. Validate:

- authorization and beneficiary scope;
- per-file and total size;
- content signature/magic bytes;
- allowed business category;
- filename/display safety when shown to users;
- storage/download access rules.

Current upload consumers:

- profile photo: JPG/PNG, 5 MB business limit;
- appointment medical order: JPG/PNG/PDF, 5 MB business limit;
- reimbursement documents/previews/pendency documents: JPG/PNG/PDF, 2 MB per file and 20 MB total.

Transport limits must stay above the largest business total with reasonable multipart overhead
(DL-0032).

Physical content uses `domain.upload.FileStorage` (SPEC-0019/ADR-0023). New writes select
PostgreSQL binary, filesystem or S3 via `FKMED_STORAGE_BACKEND`; provider-qualified references keep
old content readable after a backend change. Dev defaults to `/fkmed/uploads`, backend integration
tests to PostgreSQL and prod to private encrypted S3. Client filenames are metadata only and never
form paths or object keys. Future production hardening still needs malware scanning/quarantine.

## Notifications

Notifications are business-facing side effects. In-app notification is the durable user channel;
e-mail is best effort according to user preferences and mandatory security events. Notification
content must avoid full CPF/CNS/bank/clinical detail unless a spec explicitly allows it.

## AI / DSS

No AI/DSS module exists in the FKMed POC. If one is introduced, it must be deterministic or
human-reviewed by design, isolated behind explicit ports, observable and auditable. Model output
must never directly change business state without deterministic validation and a spec.
