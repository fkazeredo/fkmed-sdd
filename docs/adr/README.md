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
