# Slice reports

Per-slice working reports written by the architect (owner rule). Two kinds:

| Folder | Written at | Content | Versioned? |
|---|---|---|---|
| `plans/` | `/slice` (after the owner approves the plan) | The approved slice plan, including the numbered acceptance criteria (AC-1…) | **No** — gitignored (`docs/reports/plans/` in `.gitignore`) |
| `final/` | `/dod` (before the push/PR) | Conclusion report in pt-BR: AC table (evidence + detailed whys) + workflow retrospective (handoff timeline, reworks and reasons, bottlenecks, lessons learned) + the standard final-report content | **Yes** — committed on the slice branch, part of the PR |

## Naming

```
plans/YYYY-MM-DD-<slice-slug>-plan.md
final/YYYY-MM-DD-<slice-slug>-final.md
```

`<slice-slug>` is the same kebab slug used in the `feature/<slice-slug>` branch.

## Rules

- Only conclusion reports reach git — plan reports are local working artifacts and never
  pushed (owner decision). Do not "rescue" a plan report into a commit.
- The conclusion report is committed **before** `git push` + `gh pr create`, so the PR
  carries it.
- Process references: `docs/architecture/workflow.md` §Slice reports,
  `.claude/agents/architect.md` §Persisted reports, `.claude/skills/slice/SKILL.md`,
  `.claude/skills/dod/SKILL.md`.
