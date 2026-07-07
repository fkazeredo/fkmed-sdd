# ROADMAP — FKMed

## Delivery norm (owner decision — binding)

Every implementation plan is **end-to-end**: each product slice is vertical (migration →
domain → API → screen when applicable) and **each phase closes with a working front+back user
journey of real value**. No backend-only, screens-only or docs-only deliveries of product
code unless the work is inherently technical or explicitly requested. A slice is built by the
main Claude executor by default; agents are used for spec/design, review or risk-based QA,
not as parallel developers. Phase closure criteria:
journey running on the dev environment + all gates green + `docs/MANUAL.md` updated + PR
opened to `develop`.

## Phases (specs → demonstrable deliverable)

| Phase | Specs | Demonstrable deliverable that closes the phase |
|---|---|---|
| **0 — Esqueleto no ar** | 0001 | System up via compose; health/version; dev login; **"Meu Plano"** screen with seeded family data. Release **0.1.0**. |
| **1 — Entrar no portal** | 0002, 0003, 0005 | MARIA and PEDRO create their accounts (e-mail verification), log in, see the Home with the beneficiary card and switch the active beneficiary. |
| **2 — Minha conta e identificação** | 0004, 0006, 0007 | Notification center (bell) working; profile (photo, contacts, versioned terms); digital card with PDF — account management + care identification journey. |
| **3 — Encontrar atendimento** | 0008, 0009 | Find a provider (funnel + name search) and book/cancel/reschedule consultations and exams with real slot capacity. |
| **4 — Cuidado digital** | 0010, 0011 | Pronto Atendimento queue → room → closure issuing a prescription visible in Minha Saúde; scheduled teleconsultation. |
| **5 — Plano e finanças** | 0012, 0018, 0013, 0014 | Guide authorized (via operator-sim) notifies and shows password; token generation; invoices + validator + PIX code + copay + IR + settlement declaration; channels/FAQ. |
| **6 — Reembolso (POC target)** | 0015, 0016, 0017 | Full reimbursement journey: request → automatic analysis → pendency resolution → approval → payment (with glosa) → statement; preview with disclaimer. |

Order inside each phase follows dependency (e.g. 0018 lands with 0012 so guide journeys
are demonstrable). Notifications (0004) intentionally lands in phase 2: phases 0–1 send
account e-mails directly through the identity flows; 0004 then centralizes the mechanism.

## Standing rules

- A slice starts only via `/slice` over an **Approved** spec with no blocking Open
  Questions; it closes only via `/dod` (evidence + gates + docs/status + PR).
- The owner merges every PR (agents never merge/tag/force-push — DECISIONS-BASELINE §0023).
- ADRs expected during phase 0: Modulith module map; dev e-mail delivery (e.g. MailHog);
  upload file storage.
- Consciously out of the POC (recorded in the specs' Out of Scope): waiting-period
  (carências) consultation, online plan cancellation, 2FA, online payment, native mobile
  app, operator UI.
