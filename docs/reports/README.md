# Workflow reports

This folder stores workflow evidence when it is useful. Historical reports remain as the
record of how earlier FKMed slices were executed; do not rewrite them.

Lean SDD no longer requires a versioned conclusion report for every slice.

## Folders

| Folder | Use | Versioned? |
|---|---|---|
| `plans/` | Optional local working plans for large slices. Prefer the conversation checklist for normal slices. | No - gitignored |
| `final/` | Optional retrospectives for complex/risky slices, incidents, or owner-requested evidence packages. Existing files are historical evidence. | Yes, when intentionally created |

## Naming

```text
plans/YYYY-MM-DD-<slice-slug>-plan.md
final/YYYY-MM-DD-<slice-slug>-final.md
```

## Rules

- Do not create reports by default. Use them when they reduce future confusion.
- Do not rewrite historical reports to fit the new process.
- A normal slice closes through `/dod`, `docs/ROADMAP-STATUS.md`, gates and a PR.
- A complex retrospective should include acceptance evidence, what went wrong/right,
  rework, commands, decisions and next workflow adjustments.
