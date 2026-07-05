# 0004 - Notifications

**Status:** Approved

## Goal

One notification mechanism for the whole product: domain events from any module become
**in-app notifications** (bell + notification center) and **e-mails**, with per-event-type
channel preferences — so beneficiaries never miss a state change (reimbursement paid, guide
authorized, appointment confirmed) and never receive sensitive data over a channel.

## Scope

- Notification center screen (list, read/unread, deep links) + bell with unread counter.
- E-mail delivery driven by the same events (asynchronous, after commit).
- Notification event-type catalog as registry data.
- Notification preferences (e-mail opt-out per event type; mandatory types locked).
- Product-wide rule: no full sensitive data in any notification content.

## Business Context

Every module's spec defines notification triggers (e.g. SPEC-0016 reimbursement
transitions, SPEC-0012 guide status changes). This spec owns the shared mechanism and the
user experience. Triggers target either a **user account** (identity events) or a
**beneficiary** (business events); beneficiaries may not have their own account (minors).

## Business Rules

- **BR1** — A notification MUST have title, short text, date/time, read/unread state and,
  when applicable, a deep link to the resource inside the portal.
- **BR2** — The bell counter MUST reflect the number of unread notifications of the logged
  user and update immediately when items are marked read.
- **BR3** — Notifications MUST be listed newest first, paginated.
- **BR4** — Notification content MUST NOT contain full sensitive data: monetary values MAY
  appear; CPF, CNS and bank details MUST NOT (masked forms allowed where a spec requires).
- **BR5** — Notification event types are **registry data** (code, description, default
  channels, mandatory flag) seeded by migration — never a business enum (baseline §0019).
- **BR6** — E-mail delivery MUST be asynchronous (AFTER_COMMIT) and a delivery failure MUST
  NOT fail or roll back the business transaction that raised the event.
- **BR7** — The user MAY opt out of the **e-mail** channel per event type; event types
  flagged **mandatory** (security/account events: password changed, account locked,
  contact-data changed) MUST NOT be disableable. In-app delivery is always on.
- **BR8** — Business events targeted at a beneficiary MUST be delivered to that
  beneficiary's account; delivery for beneficiaries without an account follows OQ1.

## Input/Output Examples

- `GET /api/notifications?page=0` → `200 {"unread":2,"items":[{"id":"…","type":"reimbursement.paid",
  "title":"Reembolso pago","body":"Seu reembolso RE-20260601-0001 foi pago: R$ 120,00.",
  "link":"/reembolso/RE-20260601-0001","createdAt":"…","read":false}, …]}`.
- `POST /api/notifications/{id}/read` → `204`; unknown/foreign id → `404`
  `{"code":"notification.not-found"}` (error case).
- `PUT /api/notifications/preferences` disabling a mandatory type → `422`
  `{"code":"notification.preference-mandatory"}` (error case).

## API Contracts

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/notifications` | Paginated list + unread count |
| POST | `/api/notifications/{id}/read` | Mark one as read |
| POST | `/api/notifications/read-all` | Mark all as read |
| GET / PUT | `/api/notifications/preferences` | Per-event-type e-mail opt-out |

## Events

**Consumes** the domain events each spec declares as notification triggers (in-process,
AFTER_COMMIT). Publishes none.

## Persistence Changes

Migration (number at implementation): `notification` (id, account_id, event_type_code,
title, body, link, created_at, read_at); registry `notification_event_type` (code,
description, email_default, mandatory) seeded with the catalog referenced by the other
specs; `notification_preference` (account_id, event_type_code, email_opt_out).

## Validation Rules

Preference updates only for existing event types; mandatory types reject opt-out (BR7).
Title/body length limits enforced at creation (title ≤ 120, body ≤ 500).

## Error Behavior

| Failure | Code (i18n key) | HTTP |
|---|---|---|
| Notification not found / not owned | `notification.not-found` | 404 |
| Opt-out of mandatory type | `notification.preference-mandatory` | 422 |

## Observability Requirements

Counters: notifications created, e-mails sent/failed per event type. E-mail addresses
masked in logs. Failed deliveries logged with correlation id for retry diagnosis.

## Tests Required

- **Domain/unit:** preference resolution (default × opt-out × mandatory).
- **Integration (Testcontainers):** event → notification row + e-mail dispatched; failure
  of the mail sender does not roll back the business transaction.
- **API contract:** all endpoints and error codes.
- **Frontend unit:** bell counter, list states (loading/empty/error), read toggle.
- **E2E:** a business action produces the in-app item (unread) and the e-mail on the
  isolated stack's mail catcher.

## Acceptance Criteria

- **AC1** (BR1, BR2) — Given an event that notifies (e.g. reimbursement paid), then the
  e-mail is sent to the user's contact address and the item appears in the bell as unread.
- **AC2** (BR2) — Given 2 unread items, when I mark both read, then the counter shows 0.
- **AC3** (BR7) — Given I opt out of e-mail for "guide status changed", when a guide
  changes status, then no e-mail is sent but the in-app item still appears.
- **AC4** (BR7) — Given the "password changed" type, when I try to disable it, then the
  update is refused (error case).
- **AC5** (BR4) — Given any notification of the catalog, then its content contains no full
  CPF/CNS/bank data (content review test over templates).

## Open Questions

*(all resolved by the owner on 2026-07-05 — Phase 2 planning)*

- **OQ1** — RESOLVED — Recipient when the target beneficiary has no account (minors):
  deliver to the **titular's account** (the accepted proposed default). The listener routes
  a beneficiary-targeted event without an own account to the titular's account (BR8).
- **OQ2** — RESOLVED — In-app retention window: keep **12 months visible, no hard delete**
  in the POC (the accepted proposed default). Listing filters to the last 12 months by
  `created_at`; nothing is physically deleted.

## Out of Scope

Push/SMS/WhatsApp channels; notification digests; operator-authored broadcast messages
(banners/notices are SPEC-0005 content); real-time websocket delivery (polling/refresh is
acceptable in the POC except where a spec demands near-real-time, e.g. telemedicine queue).
