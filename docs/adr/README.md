# ADR Index — FKMed

Architecture Decision Records of this project. **Inherited, pre-accepted decisions** live in
[`docs/DECISIONS-BASELINE.md`](../DECISIONS-BASELINE.md) — an inherited rule is revised only
via a **new ADR citing the baseline number**. New ADRs are created via `/adr` from
[`0000-adr-template.md`](0000-adr-template.md).

| ADR | Title | Status |
|---|---|---|
| [0001](0001-modulith-module-map.md) | Initial Modulith Module Map — plan module + error kernel | Proposed |
| [0002](0002-csrf-disabled-on-stateless-api.md) | CSRF disabled on the stateless JWT `/api/**` chain | Proposed |
| [0003](0003-single-tenant-per-build.md) | Single-tenant per build — drop the multi-tenant seam (revises baseline §0003) | Proposed |
| [0004](0004-dev-email-delivery-mailpit.md) | Dev e-mail delivery — Mailpit + Spring Mail behind a `MailSender` port | Proposed |
| [0005](0005-session-idle-windows-remember-me.md) | Two session idle windows via Spring Session remember-me (BR12) | Proposed |
| [0006](0006-content-module-home.md) | Module map revision — `domain.content` (Home banners and notices, SPEC-0005) | Proposed |
| [0007](0007-pdf-generation-openpdf.md) | Server-side PDF generation library — OpenPDF (SPEC-0007) | Proposed |
| [0008](0008-notification-module.md) | Module map revision — `domain.notification` (shared in-app + async e-mail, SPEC-0004) | Proposed |
| [0009](0009-jwt-desensitize-remove-card-claim.md) | De-sensitize the JWT — remove the beneficiary card number from token claims (CodeQL) | Proposed |
| [0010](0010-card-module-digital-card.md) | Module map revision — `domain.card` (Digital Card, SPEC-0007) | Proposed |
| [0011](0011-network-module.md) | Module map revision — `domain.network` (provider network search, SPEC-0008) | Proposed |
| [0012](0012-appointment-module.md) | Module map revision — `domain.appointment` (scheduling + capacity + state machine, SPEC-0009) | Proposed |
| [0013](0013-clinical-documents-module.md) | Module map revision — `domain.clinicaldocs` (Minha Saúde clinical documents, SPEC-0011) | Proposed |
| [0014](0014-telemedicine-module.md) | Module map revision — `domain.telemedicine` (Pronto Atendimento queue + tele sessions, SPEC-0010) | Proposed |
| [0015](0015-telemedicine-room-state-driven.md) | Telemedicine room is state-driven (no real audio/video) in the POC (SPEC-0010 OQ1) | Proposed |
| [0016](0016-sse-queue-transport.md) | Server-Sent Events (SSE) for the telemedicine queue's near-real-time state (SPEC-0010 BR6) | Proposed |
| [0017](0017-operator-simulation-tele-slice.md) | Operator-simulation seam — the telemedicine+documents slice of SPEC-0018 (Phase 4) | Proposed |
| [0018](0018-guides-module.md) | Module map revision — `domain.guides` (authorization guides + attendance token, SPEC-0012) | Proposed |
| [0019](0019-support-module.md) | Module map revision — `domain.support` (service channels, antifraud, FAQ, Libras, SPEC-0014) | Proposed |
