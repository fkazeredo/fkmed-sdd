# RUN-PHASE — authorized autonomy and the decision-log format

Rules for when the owner explicitly authorizes autonomous execution ("pode decidir sozinho" /
"run this autonomously"). Outside that authorization, the default ALWAYS applies: **stop and
ask the owner** (CLAUDE.md invariant 3).

## Autonomous decision rule

When a gap or Open Question blocks authorized autonomous work: (1) prefer the recommendation
already recorded in the roadmap/spec; (2) otherwise research and pick the most defensible
value — and mark the decision **Confidence=Low**; (3) record the decision in
`docs/decision-log/` **BEFORE writing any code that depends on it**; (4) move the spec item
from `Open Questions` to `Business Rules`, marked `ASSUMED (see DL-NNNN)`; (5) report the
decision to the owner immediately — never only at the end.

## docs/decision-log/

One append-only file per decision: `DL-0001`, `DL-0002`, … Keep `docs/decision-log/INDEX.md`
listing all of them, **highlighting at the top** the ones with Reversibility=Costly or
Confidence=Low (with a "why highlighted" column). Each `DL-NNNN-title.md` contains:

- **Header:** Phase/slice, Spec(s) (with the affected BRs), related ADR (if any), Date,
  Status=ASSUMED, Confidence (High/Medium/Low), Reversibility (Cheap/Moderate/Costly).
- **Gap:** what was not decided.
- **Decision:** what was adopted (value, formula or modeling).
- **Justification:** why; cite the roadmap recommendation or the sources researched.
- **Alternatives discarded:** each one and the reason.
- **Impact:** specs, files, migrations and contracts affected.
- **How to revert:** what to change and the size of the refactoring.

The log is append-only: a revised decision gets a NEW DL referencing the old one; the
original is never rewritten beyond a revision note.

## Non-negotiable during autonomous runs

- ArchUnit, Spring Modulith, Spotless/Checkstyle and CI stay armed and able to break the
  build. Never loosen, skip or delete a test or gate to make code pass.
- One Flyway migration per schema change (idempotent; never edit an applied one).
- `DomainException.code` == i18n key; messages in every product locale.
- No raw persistence exceptions leaking; OpenAPI snapshot kept in sync.
- Business events logged, personal data masked, correlation id present.
- No cross-context FKs (another context's id is a value); events in-process, AFTER_COMMIT.
- Git: local commits on the feature branch; slice green ⇒ push + PR to `develop`; never
  merge/tag/force-push.
