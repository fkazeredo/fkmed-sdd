# Parameterization — what to preserve, parameterize and reset

Bootstrap checklist. Three possible destinations for each template artifact.

## ✅ Preserve as-is (the METHOD — do not touch)

| Artifact | Why |
|---|---|
| `docs/specs/0000-specs-template.md` | Spec template — the method's contract |
| `docs/adr/0000-adr-template.md` | ADR template |
| `docs/architecture/*` | The architecture rules (all areas) |
| `docs/TUTORIAL.md` | The 7-step loop |
| `docs/RUN-PHASE.md` | Canonical decision-log format (read by /dl) |
| The whole `.claude/` (skills, agents, settings.json) | The team toolkit travels with the template |
| Build gates (ArchUnit, Checkstyle, Spotless, Modulith, JaCoCo, PIT in the pom) | Tooling is authoritative (invariant 5) |
| `CONTRIBUTING.md`, `SECURITY.md`, PR template | Governance (DECISIONS-BASELINE §0023) |
| `docker-compose.yml`, `compose.e2e.yaml`, `compose.prod.yaml` | Local/E2E/prod infra |
| `.github/workflows/*` | CI from day one |
| `.gitignore`, `.pre-commit-config.yaml` | Secret protection |

## 🔧 Parameterize (change the value, keep the structure)

| Artifact | What to change |
|---|---|
| Java base package (`com.example.product` (the template placeholder)) | → the new product's package; adjust ArchUnit/Modulith/Checkstyle citing it |
| Product strings (name, branding) | OpenAPI title, frontend branding (NavItem/logo), READMEs |
| `backend/pom.xml` | `artifactId`, `name`, initial version `0.1.0` |
| Default ports | Only if they collide in the new team's environment (`.env.example`) |
| Docker/GHCR images | Image names in `docker-publish.yml` and compose.prod |
| `.env.example` / `.env.prod.example` | Product-specific variables |
| `.gitleaks.toml` (allowlist) | Review the enumerated dev-defaults; drop the ones the new product does not use |
| `.github/CODEOWNERS` | The new team's real handles |
| Inherited structural ADRs | KEEP the method ones (modular monolith, SemVer 0015, cadastro-vs-enum 0019, cache 0022, governance 0023…) with a "inherited from the template" note; DISCARD the original product's specific ones |

## 🗑️ Reset (PRODUCT artifacts — zero them for the new product)

| Artifact | Action |
|---|---|
| `docs/specs/0001+` | Delete; the new SPEC-0001 is born via `/spec` |
| `docs/decision-log/*` | Zero; a new DL-0001 at the first autonomous decision |
| `docs/ROADMAP.md`, `docs/ROADMAP-STATUS.md` | New product roadmap |
| `docs/DOMAIN.md`, `docs/event-storming.md` | New domain |
| `docs/MANUAL.md` + `docs/MANUAL.en-US.md` | Skeleton (section structure, content zeroed) |
| `docs/release-notes/CHANGELOG*.md` | Zeroed with the new header |
| `docs/manual/img/` | Empty it (the new product's screenshots come from the script) |
| `docs/api/openapi.json` | Regenerate (`-Dopenapi.snapshot.write=true`) after the skeleton |
| `README.md` + `README.en-US.md` | Rewrite for the new product (keep the language selector) |
| Domain code in `backend/` and screens in `frontend/` | The minimal runnable skeleton replaces the template's domain, per workflow.md §New project |

## Future — plugin migration trigger (do not build now)

While there is ONE active project, `.claude/` travels copied with the template and evolves in
each repo. **Review trigger**: when **2+ active projects** use the toolkit and a fix in a
skill/agent needs to propagate between them, migrate `skills/` + `agents/` to a Claude Code
**plugin** (a `plugin.json` manifest) distributed via git URL or a private marketplace,
leaving in each repo only what is project-specific. Until then, a plugin would be ceremony
(Rule Zero).
