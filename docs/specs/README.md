# Spec Index — FKMed

Specs follow [`0000-specs-template.md`](0000-specs-template.md) (created via `/spec`),
in **en-US** (project language policy), with testable Business Rules and Given/When/Then
Acceptance Criteria. A spec is a living artifact: it changes in the same PR as the code it
governs. Implementation of a spec is blocked while it has Open Questions affecting the
slice (`/slice` gate).

| Spec | Title | Module / area | Status |
|---|---|---|---|
| [0001](0001-walking-skeleton.md) | Walking Skeleton — "Meu Plano" | platform / plan | Approved |
| [0002](0002-identity-and-access.md) | Identity and Access | identity | Approved |
| [0003](0003-beneficiary-context-and-authorization.md) | Beneficiary Context and Authorization | cross-cutting | Approved |
| [0004](0004-notifications.md) | Notifications | notifications | Draft |
| [0005](0005-home.md) | Home | home / content | Approved |
| [0006](0006-profile-and-account.md) | Profile and Account | profile | Draft |
| [0007](0007-digital-card.md) | Digital Card | card | Draft |
| [0008](0008-provider-network-search.md) | Provider Network Search | network | Draft |
| [0009](0009-appointments.md) | Appointments | scheduling | Draft |
| [0010](0010-telemedicine.md) | Telemedicine | telemedicine | Draft |
| [0011](0011-clinical-documents.md) | Clinical Documents (Minha Saúde) | clinical-docs | Draft |
| [0012](0012-guides-and-tokens.md) | Guides and Tokens | guides | Draft |
| [0013](0013-plan-finance.md) | Plan Finance | finance | Draft |
| [0014](0014-service-channels-and-faq.md) | Service Channels and FAQ | support | Draft |
| [0015](0015-reimbursement-request.md) | Reimbursement Request | reimbursement | Approved |
| [0016](0016-reimbursement-analysis-and-tracking.md) | Reimbursement Analysis and Tracking | reimbursement | Approved |
| [0017](0017-reimbursement-preview.md) | Reimbursement Preview | reimbursement | Approved |
| [0018](0018-operator-simulation-api.md) | Operator Simulation API | operator-sim (dev-only) | Approved |

## Product-wide UI norms (referenced by every spec)

Normative for every screen; specs assume these without repeating them:

1. **Screen states** — every data-loading screen MUST handle: loading, empty (orientative
   message + action when it makes sense), error (message + retry) and success.
2. **Write feedback** — every write action MUST give visible feedback (confirmation or
   error) and MUST prevent duplicate submission on double click.
3. **Formats** — dates `dd/mm/aaaa`, time `HH:mm`, currency `R$ 1.234,56`,
   CPF/CNPJ/phone masked inputs. Display timezone: `America/Sao_Paulo`.
4. **Lists** — long lists MUST paginate or load incrementally, ordered most-recent-first
   unless the spec says otherwise.
5. **Accessibility** — keyboard navigation, labels on fields and icon-only buttons,
   adequate contrast.
6. **Privacy** — CPF, CNS and bank data masked outside explicitly allowed contexts;
   personal data never in URLs (SPEC-0003 BR8).
7. **Product locale** — the UI is **pt-BR**; every user-facing string lives in the i18n
   bundle (completeness gate).

## Glossary (product domain)

| Term | Definition |
|---|---|
| **ANS** | National Supplementary Health Agency; also the product's registration number |
| **CNS** | National Health Card (15 digits) |
| **Carteirinha** | Beneficiary's plan identification (9 digits in this POC) |
| **Abrangência** | Plan's geographic coverage: municipal, state or national |
| **Rede credenciada** | Accredited providers (doctors, clinics, hospitals, labs) |
| **Guia** | Procedure-authorization request opened by the provider; the beneficiary tracks its status |
| **Token de atendimento** | Short-lived 6-digit code presented at reception to validate care (antifraud) |
| **Coparticipação** | Amount the beneficiary pays per plan usage |
| **Reembolso (livre escolha)** | Refund of an expense paid to an out-of-network provider, limited to the plan table |
| **Prévia de reembolso** | Non-binding estimate of the reimbursable amount |
| **Glosa** | Total or partial value cut in analysis (guide or reimbursement), always with a reason |
| **TUSS** | Unified terminology of health procedures (codes) |
| **Titular / Dependente** | Contract holder / family member linked to the titular |

## Canonical reference data

Seeded by migrations (owners: SPEC-0001 plan/family; each module its own data): plan
`PLANO MÉDICO — ADESÃO PRATA RJ QP COPART TP` (ANS `326305`, Estadual RJ, copay,
reimbursement); titular **MARIA CLARA SOUZA LIMA** (`001234567`) and dependent **PEDRO
SOUZA LIMA** (`001234575`); reimbursement table (Consulta R$ 120,00 · Exame R$ 80,00 ·
Terapia/Psicologia R$ 60,00/session · Honorários R$ 900,00 · multiple 1.0). Names, card
numbers, CPFs and contacts are **fictitious** POC reference mass.
