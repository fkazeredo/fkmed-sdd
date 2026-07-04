# Frontend Architecture — Angular

> Read when: writing or changing any Angular code, UI behavior, forms, state, HTTP or
> keyboard/UX handling.

## Stack (final — v0.51.1)

| Concern | Choice | Version |
|---|---|---|
| Framework | Angular (standalone components, **zoneless**, signals) | 22.0.x |
| UI kit | PrimeNG (preset **Aura** via `@primeuix/themes` 2.0.x) + primeicons 7 | 21.1.x |
| Utility CSS | Tailwind CSS (integrated with PrimeNG through CSS layers) | 4.3.x |
| A11y/behavior primitives | Angular CDK | 22.0.x |
| i18n | `@ngx-translate/core` with an **in-memory loader** (`core/i18n/translations.ts`, default = the product's primary locale) | 18 |
| Auth | `angular-oauth2-oidc` — OIDC Authorization Code + **PKCE** against the embedded AS; silent refresh via iframe | 20.0.x |
| Unit tests | Vitest (+ `@vitest/coverage-v8`; jsdom) | 4.x |
| E2E | Playwright against the **isolated** stack (`compose.e2e.yaml`, port 4201) | 1.61.x |
| Lint/format | angular-eslint 22 + ESLint 10 + Prettier 3 | — |
| Language | TypeScript | 6.0.x |

Gates (all must stay green): `npm run lint`, `npm test` (coverage floors: statements 70,
lines 75, functions 49, branches 55), `ng build`.

## Structure (real)

Organize by feature/domain with controlled `core` and `shared`:

```txt
src/app
  core/      auth (OIDC config, guards, interceptor)  http  i18n (in-memory translations)
             layout (shell: sidebar, topbar, command palette)  shortcuts (ShortcutService)
             feedback (FeedbackService → global p-toast)
  shared/    components (app-screen-state, ...)  pipes (registryLabel, ...)  validators  utils
  features/  <one folder per backend module/screen — e.g. orders  customers  billing
             registry  identity  dashboard  login  health>
```

`core` = app-wide infrastructure and singletons. `shared` = genuinely reusable UI/utils.
Feature-specific code **MUST** stay inside the feature.

## Product and UX (corporate, keyboard-first)

Frontend is product in use. Decision priority: product requirements > UX spec > customer
expectations > approved designs > design system > acceptance criteria > accessibility >
existing patterns > Angular code organization. The UI represents the user workflow, not the
database model.

Standing UX invariants of this app (do not regress):

- **Keyboard-first:** every screen has a curated, unique `g + <key>` shortcut; the `?`
  dictionary and the `Ctrl/Cmd+K` command palette derive from the **same** registry that
  navigates (they can never disagree). Single-key shortcuts have a persisted **off toggle**
  (WCAG 2.1.4). Contextual per-screen commands register on init and dispose on destroy.
- **`Esc` climbs one layer at a time** (open dialog → editable field blur → `Location.back()`
  guarded); **`Enter` submits** every mutation form (`ngSubmit`/`keyup.enter`).
- **Unsaved-work guard:** every mutation screen implements `isDirty()` + `canDeactivate`.
- **Real states everywhere:** loading, empty, error, validation, permission denied — via the
  shared `<app-screen-state>` component; disabled submit while processing.
- **Success feedback by toast** (global `p-toast` host + `FeedbackService`); **errors stay
  inline**, next to the field that failed.
- Motion respects `prefers-reduced-motion`; focus uses `:focus-visible` tokens; light/dark
  themes via `--app-*` tokens.

## State management

Pragmatic and proportional. No global store: local component state (signals) for simple UI
state; feature services for state shared inside a feature; RxJS or signals by clarity. NgRx
or similar only for real complexity — this app never needed it.

## Components and forms

Pages/feature components **MAY** orchestrate; reusable components are presentation-oriented
with inputs/outputs and minimal business coupling. Template-driven forms with `ngModel` are
the project norm for the operational screens; extract builders/mappers/validators when forms
grow. Confirmation before destructive actions.

## HTTP and errors

Hybrid approach: `core` owns base URL, auth token propagation, correlation ID, global error
normalization and interceptors. Each feature exposes a domain-oriented API service returning
typed responses; list endpoints use the backend's `PageResponse` envelope. No raw
`HttpClient` scattered in components; no generic API client hiding domain intention.

Error handling combines global normalization with feature-specific presentation (inline
error near the field, error state, permission screen). Do not force every error into a
generic toast — toasts are for **success** feedback in this app.

## UI libraries and styling

PrimeNG (Aura) + Tailwind v4 is the standard — never casually mix other libraries. Component
styles stay close to the component; global styles hold only design tokens (`--app-*`,
elevation/motion) and layer wiring. User-facing text always goes through ngx-translate
(labels, buttons, tables, empty states, dialogs, toasts, display names, dates/currencies) —
the translation source is `core/i18n/translations.ts`, kept in full parity across the
product's locales.
